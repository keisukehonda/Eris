package net.khonda.eris.node

import net.khonda.eris._
import net.khonda.eris.config.{Eris => ErisConfig}
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
                 node_state: Status,
		 db_state: Status,
		 dbhost: String,
		 dbport: Int,
		 score: Int) extends NodeMessage

case class RoutingTable(version: Long = 0L,
			table: List[Route] = List.empty,
			unreachable: Set[Route] = Set.empty) extends NodeMessage {

  def addresses: IndexedSeq[Address] = (table map (n => AddressFromURIString(n.uri))).toIndexedSeq

}


class Node(config: ErisConfig) {
  import Status._

  val logger = LoggerFactory.getLogger(classOf[Node])  
  val startTime =  java.util.Calendar.getInstance(new java.util.Locale("ja", "JP", "JP")) //TODO
  val akkaConfig = ConfigFactory.load()
  val port = ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
  val system = ActorSystem("ChordSystem-"+port,
			   ConfigFactory.load().getConfig(config.app_no).withFallback(akkaConfig))

  val failureDetector = 
    new AccrualFailureDetector(system, 
			       config.failuredetector_threshold, 
			       config.failuredetector_maxSampleSize, 
			       config.failuredetector_minStdDeviation, 
			       config.failuredetector_acceptableHeartbeatPause, 
			       config.failuredetector_firstHeartbeatEstimate, 
			       AccrualFailureDetector.realClock)
  val router = Router(system, config, failureDetector)
  // private val selfHeartbeat = Heartbeat(router.self)  
  val lookupProxy = new LookupProxy(system, router)
  val stabilizer = new Stabilizer(system, router)

  //gossip tick
  private lazy val gossipTask = FixedRateTask(system.scheduler, 0 milliseconds, 2000 milliseconds) {    
    gossipTick()
  }

  //join to cluster using lookup node and check i am newcomer or rejoin
  val (accept, isReJoin) = join(AddressFromURIString(config.getUri(config.lookup._1, config.lookup._2)))
  
  //
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
	//validation nodeid
	if(router.nodeid.matches("""^[a-z0-9]{40}$""")) {
	  //join and stabilizer start	
	  logger.info("join as:"+router.nodeid)
	  if(stabilizer.join(router.getSuccessor)) gossipTask
	  (true, isReJoin)
	} else { logger.info("id is not valid:"+router.nodeid); (false, false) }	
      }
      case None => { logger.info("lookup fail"); (false, false) }
    }
  }

  def gossipTick(): Unit = {    
    stabilizer.gossip()
  }
  
}

object Router {

  def apply(system: ActorSystem, config: ErisConfig, failureDetector: FailureDetector) = {
    new Router(system, config, failureDetector)
  }

}

class Router private (system: ActorSystem, config: ErisConfig, failureDetector: FailureDetector) {
  import Status._

  val logger = LoggerFactory.getLogger(classOf[Router])
  
  case class State(rt: RoutingTable)

  val state = {
    val latest = RoutingTable()
    new AtomicReference[State](State(latest))
  }

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
      case e:Exception => logger.info(e.getMessage()); ""
    } 
  } else ""

  lazy val node: Route = {
    val id = nodeid
    val score = 1
    val db = config.db
    val node_state = Status.Joining
    val db_state = Status.Joining
    Route(id, self.toString, config.port, node_state, db_state, db._1, db._2 , score)
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
    //if (newone.uri != self.toString) failureDetector heartbeat AddressFromURIString(newone.uri)
  }

  def changeState(address: Address, newState: Status): Unit = {
    val localTable = currentTable.table
    val localUnreachable = currentTable.unreachable

    val newTable = localTable map {
      route => if(route.uri == address.toString) {
	route copy (node_state = newState)
      } else route
    }
    
    val newUnreachable = {
      if(newState == Joining) localUnreachable filterNot { route => route.uri == address.toString } else localUnreachable
    }    
    updateRoutingTable(RoutingTable(System.currentTimeMillis(), newTable, newUnreachable))
  }

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

  def getSuccessor: Route = getSuccessor(getNode)  

  def getSuccessor(target: Route): Route = {
    def cond(route: Route) = { 
      //TODO apply db state
      val state = route.node_state
      (state != Leaving && state != Exiting && state != Removed)
    }
    getSuccessor(target, cond)
  }

  def getSuccessor(target: Route, condition: Route => Boolean): Route = {
    val localTable = currentTable.table
    if(localTable.isEmpty) target else {
      //split
      val splits = localTable.splitAt(localTable.indexWhere(route => route.uri == target.uri) + 1)
      if(splits._2.isEmpty) {
	//localTable.head 
	localTable.find(route => condition(route)) match {
	  case Some(route) => route
	  case None => target
	}
      } else {
	//splits._2.head
	splits._2.find(route => condition(route)) match {
	  case Some(route) => route
	  case None => splits._1.find(route => condition(route)) match {
	    case Some(route) => route
	    case None => target
	  }
	}	
      }
    }
  }

  def getPredecessor: Route = getPredecessor(getNode)
  
  def getPredecessor(target: Route): Route = {    
    def cond(route: Route) = { 
      val state = route.node_state
      (state != Leaving && state != Exiting && state != Removed)
    }
    getPredecessor(target, cond)
  }

  def getPredecessor(target: Route, condition: Route => Boolean): Route = {    
    val localTable = currentTable.table    

    if(localTable.isEmpty) target else {
      //split
      val splits = localTable.splitAt(localTable.indexWhere(route => route.uri == target.uri))
      if(splits._1.isEmpty) {		
	splits._2.reverse.find(route => condition(route)) match {
	  case Some(route) => route
	  case None => target
	}
      } else {
	splits._1.reverse.find(route => condition(route)) match {
	  case Some(route) => route
	  case None => splits._2.reverse.find(route => condition(route)) match {
	    case Some(route) => route
	    case None => target
	  }
	}
      }
    }
  }

  def receiveHeartbeat(from: Address): Unit = failureDetector heartbeat from
  
}
