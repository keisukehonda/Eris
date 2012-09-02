package jp.ne.internavi.sync.hcp.dist

import akka.actor._
import akka.remote.RemoteScope
import akka.util.duration._
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.xml.XML


import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig}

trait NodeMessage extends Serializable

sealed trait Status extends NodeMessage
object Status {
  case object Joining extends Status  
  case object Up extends Status
  case object Down extends Status
  case object Leaving extends Status
  case object Exiting extends Status
  case object Removed extends Status
}

case class Route(id: String,
                 uri: String,
		 port: Int,
                 state: Status,
		 dbhost: String,
		 dbport: Int,
		 score: Int) extends NodeMessage

case class RoutingTable(version: Long,
			table: List[Route],
			unreachable: Set[Route]) extends NodeMessage

class Node(config: BriseisConfig) {  
  import Status._

  val logger = LoggerFactory.getLogger(classOf[Node])
  val startTime =  java.util.Calendar.getInstance(new java.util.Locale("ja", "JP", "JP")) //TODO

  val akkaConfig = ConfigFactory.load()
  val port = ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
  val system = ActorSystem("ChordSystem-"+port,
			   ConfigFactory.load().getConfig(config.app_no).withFallback(akkaConfig))
  val failureDetector = 
    new AccrualFailureDetector(system, 8.0, 10000, 100 milliseconds, 3000 milliseconds, 1000 milliseconds, AccrualFailureDetector.realClock)
  val router = Router(system, config, failureDetector)
  private val selfHeartbeat = Heartbeat(router.self)  
  val lookupProxy = new LookupProxy(system, router)
  val stabilizer =  new Stabilizer(system, router)
  val broker = new RequestBroker(config, router, lookupProxy)

  //del marker 
  lazy val delMarker = new DelMarker

  //joining picker 
  lazy val pickers: List[JoiningPicker] = {
    val localTable = router.currentTable.table
    //primaly picker
    val picker = JoiningPicker("delivery_wall_messages", 
			       router.getPredecessor, 
			       router.getNode, 
			       router.getSuccessor)
    picker.start
    localTable.size match {
      case 1 => {	
	List(picker)
      }
      case 2 => {
	List(picker)
      }
      case _ => {	
	List(picker)
      }
    }
  }

  //leaving picker
  lazy val lvPickers: List[JoiningPicker] = {
    //primaly picker
    val picker = JoiningPicker("delivery_wall_messages", 
			       router.getPredecessor, 
			       router.getSuccessor,
			       router.getNode)
    List(picker)
  }
  
  //handoff picker
  lazy val hoPicker: HandoffPicker = HandoffPicker(router.currentTable, router.getNode)

              
  //join to cluster using lookup node and check i am newcomer or rejoin
  val (accept, isReJoin) = join(AddressFromURIString(config.getUri(config.lookup._1, config.lookup._2)))
  if(accept) save_status() else { }//TODO shutdown 
  
  
  //heartbeat start
  private val heartbeatTask = FixedRateTask(system.scheduler, 3000 milliseconds, 4000 milliseconds) {
    heartbeat()
  }

  //failureDetectorReaper start
  private val failureDetectorReaperTask = FixedRateTask(system.scheduler,  2000 milliseconds, 4000 milliseconds) {
    reapUnreachableNode()
  }    

  //NodeAction start
  private val nodeActionsTask = FixedRateTask(system.scheduler, 0 milliseconds, 2000 milliseconds) {
    nodeActions()
  }

  //handoff action start
  private val handoffTask = FixedRateTask(system.scheduler, 1 minutes, 1 minutes) {
    handoffAction()
  }

  private val delmarkTask = FixedRateTask(system.scheduler, 3 minutes, 1 minutes) {
    delmarkAction()
  }

  //leaderAction start
  private val leaderActionsTask = FixedRateTask(system.scheduler, 1000 milliseconds, 2000 milliseconds) {
    leaderActions()
  }
    
  system.registerOnTermination(shutdown())
  
  //Interface NodeAction call from Thrift client
  //Join: (isAccept, isReJoin)
  def join(proxy: Address): (Boolean, Boolean) = {
    //lookup     
    lookupProxy.lookup(proxy.toString) match {
      case Some(rt) => { //get current table of cluster	
	router.updateRoutingTable(rt)
	val localTable = router.currentTable.table
	val isReJoin = localTable.find(_.uri == router.self.toString) match {
	  case Some(self_in_rt) => {
	    //check id value rt and status.xml
	    if(self_in_rt.id != router.nodeid) { router.nodeid = self_in_rt.id }
	    true
	  }
	  case None => {	    
	    if(router.nodeid != "") true else {
	      router.nodeid = router.calcHostId(router.self.toString)	      
	      false
	    }	    
	  }
	}
	//join and stabilizer start	
	if(stabilizer.join(router.getSuccessor)) stabilizer.start()
	(true, isReJoin)
      }
      case None => { logger.debug("lookup fail"); (false, false) }
    }
  }

  //Down
  def down(address: Address): Unit = {
    logger.debug("mark as down "+ address)    
    router.changeState(address, Status.Down)
  }

  //Leave
  def leave(address: Address): Unit = {
    logger.debug("mark as Leaving "+ address)
    router.changeState(address, Status.Leaving)    
  }

  //Remove
  def remove(address: Address): Unit = {
    logger.debug("remove immidiatly "+address)
    router.changeState(address, Removed)    
  }

  private def save_status(): Unit = {    
    //write node_status.xml
    val node = router.getNode
    val node_status = <node>
    <id>{node.id}</id>
    <uri>{node.uri}</uri>
    <score>{node.score}</score>
</node>
    val filename ="node_status_"+config.app_no+".xml"
    XML.save("./logs/"+filename, node_status, "UTF-8", false)
  }

  //Shutdown
  def shutdown(): Unit = {    
    logger.debug("shutdown Chord system")
    heartbeatTask.cancel()
    failureDetectorReaperTask.cancel()
    leaderActionsTask.cancel()
    handoffTask.cancel()
    delmarkTask.cancel()
    stabilizer.cancel()
    save_status()
  }
  
  //internal api
  def heartbeat(): Unit = {
    if(dbAlive) {
      val localTable = router.currentTable.table
      val beatTo = localTable.toSeq.map(_.uri). map(AddressFromURIString(_))

      for (address <- beatTo; if address != router.self) {	
	system.actorFor(address+"/user/stabilizer") ! selfHeartbeat      
      }
    }
  }

  def reapUnreachableNode(): Unit = {
    
    if(!router.isSingletonCluster){
      val localTable = router.currentTable.table
      val localUreachable = router.currentTable.unreachable
      val newlyDetectedUnreachableNodes = localTable filterNot { node => failureDetector.isAvailable(AddressFromURIString(node.uri)) }
      
      if(newlyDetectedUnreachableNodes.nonEmpty){
	logger.debug("Unreachable "+newlyDetectedUnreachableNodes)
	val newUnreachableNodes = localUreachable ++ newlyDetectedUnreachableNodes	
	//update RoutingTable
	router.updateRoutingTable(RoutingTable(System.currentTimeMillis(), localTable, newUnreachableNodes))
      }
    }
    
  }

  def leaderActions(): Unit = {

    if(router.isLeader) {
      logger.debug("leader action")
      val localTable = router.currentTable.table
      val localUnreachable = router.currentTable.unreachable
      
      val (hasStateChanged: Boolean, newTable: List[Route], newUnreachable: Set[Route]) = 
      if(localUnreachable.isEmpty){	
	val newTable = localTable filterNot { route => route.state == Removed }	
	//check update exists
	val (removedNode, newNode) = localTable partition (_.state == Removed)
	val hasStateChanged = removedNode.nonEmpty 
	(hasStateChanged, newTable, Set.empty[Route])	
      }else {	
	val newTable = localTable map {
	  //Move unreachableButNotDown => Down
	  route => if(route.state != Status.Down && localUnreachable.exists(_.uri == route.uri)) route copy (state = Status.Down)
		   else route
	}
	val (unreachableButNotDownedNodes, _) = localUnreachable partition (_.state != Status.Down)
	(unreachableButNotDownedNodes.nonEmpty, newTable, Set.empty[Route])
      }      
      if(hasStateChanged) {
	println("leader true")
	router.updateRoutingTable(RoutingTable(System.currentTimeMillis(), newTable, newUnreachable))
      }
    }

  }

  private def joiningTask(self: Route, localTable: List[Route]) = {
    //check picker task done -> update state to Up
    isReJoin match {
      case false => {
	//new join picker
	if(pickers.filter(p => p.isDone == false).isEmpty) { //all picker task done	
	  val newTable = localTable map {
	    route => if(route.uri == self.uri) route copy (state = Up) else route	
	  }
	  (true, newTable)
	} else {
	  (false, localTable)
	}
      }
      case true => {
	//new handoff picker
	if(hoPicker.isDone){
	  val newTable = localTable map {
	    route => if(route.uri == self.uri) route copy (state = Up) else route	
	  }	  
	  (true, newTable)
	} else (false, localTable)
      }
    }
  }

  private def leavingTask(self: Route, localTable: List[Route]) = {
    //check picker task done -> update state to Exiting
    if(lvPickers.filter(p => p.isDone == false).isEmpty) {
      val newTable = localTable.map {
	route => if(route.uri == self.uri) route copy (state = Exiting) else route
      }	
      (true, newTable)
    } else (false, localTable)
  }

  def activateLeavePicker(uri: String): Int = {
    lvPickers.foreach(picker => picker.start)
    1
  }

  //actions of self node
  def nodeActions(): Unit = {
    val self = router.getNode
    val localTable = router.currentTable.table
    
    val (hasStateChanged: Boolean, newTable: List[Route]) = //TODO match case statement
    if(self.state == Joining) joiningTask(self, localTable);
    else if(self.state == Leaving) leavingTask(self, localTable);
    else (false, localTable); 

    if(hasStateChanged) {      
      router.updateRoutingTable(RoutingTable(System.currentTimeMillis(), newTable, router.currentTable.unreachable))
    }
    //TODO 

  }

  //action of handoff pick
  def handoffAction(): Unit = {
    val rt = router.currentTable
    println("handoff picker restart")
    hoPicker.restart(rt) 
  }

  def delmarkAction(): Unit = {    
    println("delmarker restart")
    delMarker.update(router.getNode, router, "delivery_wall_messages")
  }

  //TODO
  def dbAlive(): Boolean = {
    true
  }  

}

object Router {

  def apply(system: ActorSystem, config: BriseisConfig, failureDetector: FailureDetector) = {
    new Router(system, config, failureDetector)
  }

}

class Router private (system: ActorSystem, _config: BriseisConfig, failureDetector: FailureDetector) {
  import Status._
  
  val logger = LoggerFactory.getLogger(classOf[Router])
  
  case class State(rt: RoutingTable)

  val state = {
    val latest = RoutingTable(0L, List.empty, Set.empty)
    new AtomicReference[State](State(latest))
  }

  //TODO state case class system, config, rt etc  
  val config = _config
  
  lazy val self: Address = {
    val port = ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
    val local = config.getUri(config.hostname, port)
    AddressFromURIString(local)
  }

  var nodeid = if(config.autoJoin) {
    try {
      val filename ="node_status_"+config.app_no+".xml"
      val status = XML.loadFile("logs/"+filename)
      val id=status \ "id"
      id.text
    } catch {
      case e:Exception => ""
    } 
  } else ""

  lazy val node: Route = {
    val id = nodeid
    val score = 1
    val db_server = config.rose_server
    val status = Status.Joining
    Route(id, self.toString, config.port, status, db_server._1,db_server._2 , score)
  }
      
  def currentTable: RoutingTable = state.get.rt

  @tailrec
  final def updateRoutingTable(newTable: RoutingTable): Unit = {
    val localState = state.get
    val localTable = localState.rt
    //check version
    val winningTable = if(localTable.version == newTable.version) {
	localTable
      }else if(localTable.version > newTable.version) {
	localTable
      }else {
	newTable
      }      
    val newState = localState copy (rt = winningTable)
    if(!state.compareAndSet(localState, newState)) updateRoutingTable(newTable) //recur if we fail the update
  }
  
  final def add(newone: Route): Unit = {    
    val localState = state.get
    val localTable = localState.rt.table
    
    if(!localTable.exists(_.uri == newone.uri)) { //newcomer
      //add newone as predessor
      val newList = localTable :+ newone
      val sortedList = newList.sortWith((r1, r2) => SHA1Hasher.compareWith(r1.id, r2.id))
      logger.debug("add "+sortedList)
      updateRoutingTable(currentTable copy (version = System.currentTimeMillis(), table = sortedList))
    }else {
      changeState(AddressFromURIString(newone.uri), Joining) //list aleady contains newone => join again      
    }
    //add heartbeat list
    if (newone.uri != self.toString) failureDetector heartbeat AddressFromURIString(newone.uri)
  }

  //Leader is head of routing table && not unreachable
  def isLeader: Boolean = (getLeader.uri == getNode.uri)

  def getLeader: Route = {
    val localTable = currentTable.table
    val localUnreachable = currentTable.unreachable
    val upList = localTable filter (route => route.state == Up && !localUnreachable.exists(route.uri == _.uri))
    if(upList.isEmpty) getNode else upList.head    
  }
  
  def isSingletonCluster: Boolean = currentTable.table.size == 1
  
  def receiveHeartbeat(from: Address): Unit = failureDetector heartbeat from

  def getSuccessor: Route = getSuccessor(getNode)
  
  def getSuccessor(target: Route): Route = {   
    val localTable = currentTable.table
    if(localTable.isEmpty) getNode else {
      //split
      val splits = localTable.splitAt(localTable.indexWhere(route => route.uri == target.uri))
      if(splits._1.isEmpty) {	
	splits._2.reverse.find(route => route.state == Up) match {
	  case Some(route) => route
	  case None => getNode
	}
      } else {
	splits._1.reverse.find(route => route.state == Up) match {
	  case Some(route) => route
	  case None => splits._2.reverse.find(route => route.state == Up) match {
	    case Some(route) => route
	    case None => getNode
	  }
	}
      }
    }    
  }

  def getPredecessor: Route = getPredecessor(getNode)

  def getPredecessor(target: Route): Route = {
    val localTable = currentTable.table
    if(localTable.isEmpty) getNode else {
      //split
      val splits = localTable.splitAt(localTable.indexWhere(route => route.uri == target.uri) + 1)
      if(splits._2.isEmpty) localTable.head else splits._2.head
    }    
  }

  def getRouteByURI(uri: String): Option[Route] = {
    val localTable = currentTable.table
    localTable.find(route => route.uri == uri)    
  }
    
  //TODO rename self
  def getNode: Route = {
    val localTable = currentTable.table
    if(localTable.isEmpty) node
    else {
      localTable.find(route => route.uri == self.toString) match {
	case Some(route) => route
	case None => node //TODO exception self node is not contained in rt
      }
    }
  }

  def changeState(address: Address, newState: Status): Unit = {
    val localTable = currentTable.table
    val localUnreachable = currentTable.unreachable

    val newTable = localTable map {
      route => if(route.uri == address.toString) {
	route copy (state = newState)
      } else route
    }
    
    val newUnreachable = {
      if(newState == Joining) localUnreachable filterNot { route => route.uri == address.toString } else localUnreachable
    }    
    updateRoutingTable(RoutingTable(System.currentTimeMillis(), newTable, newUnreachable))
  }

  def calcHostId(hostname: String): String = {    
    import java.io.StringWriter
    val localTable = currentTable.table
    val starters = List("1","9","5","d","3","b","6","e","2","a","4","c","8","7")
    
    localTable.size match {
      case n if(n < starters.length) => {
	val upper =  new StringWriter(){{ for(i <- 1 to 20) write(starters(n)) }}.toString()	
	val lower = SHA1Hasher.half(hostname.reverse+"ak5KOul.4qEms")
	upper+lower	
      }
      case _ => SHA1Hasher(hostname.reverse+"ak5KOul.4qEms") // more then just random
    }
  }

}

class RequestBroker(config: BriseisConfig, router: Router, lookupProxy: LookupProxy) {
  val logger = LoggerFactory.getLogger(classOf[RequestBroker])
 
  def genID(key: String): String = {
    SHA1Hasher(key)    
  }
  
  @tailrec
  private def localSearch(id: String, self: Route, list: List[Route], top: Route): Route = {
    val next = list.head
    if(SHA1Hasher.isInInterval(id, self.id, next.id)) next else {
      if(list.size==1) top else localSearch(id, list.head, list.tail, top) //recur
    }
  }
  
  def lookup(key: String): Route = {
    val localTable = router.currentTable.table
    val id = genID(key)
    //find primaly node
    val head = localTable.head
    val tail = localTable.tail
    if(localTable.size != 1) localSearch(id, head, tail, head) else head
  }

  def getHosts(key: String): Seq[Route] = {    
    val lpNode = lookup(key)
    List(lpNode, 
	 router.getSuccessor(lpNode), 
	 router.getPredecessor(lpNode))
  }

  def getRoseConn(route: Route): DistClient = new DistClient(route.dbhost, route.dbport)

  def getConn(host:String, port: Int): DistClient = new DistClient(host, port)

}
