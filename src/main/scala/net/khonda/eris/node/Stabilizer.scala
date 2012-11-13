package net.khonda.eris.node


import akka.actor._
import akka.dispatch.Future
import akka.dispatch.Await
import akka.util.duration._
import akka.util.Duration
import akka.util.Timeout
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.forkjoin.ThreadLocalRandom
import net.khonda.eris.config.{Eris => ErisConfig}


//INTERNL API
case class Join(from: Route) extends NodeMessage

case class Heartbeat(from: Address) extends NodeMessage

//TODO REMOVE
case object Stabilize extends NodeMessage

case class Send(to: Address) extends NodeMessage


case class Envelope(from: Route,  rt: RoutingTable) extends NodeMessage
case class Response(b: Boolean, rt: RoutingTable) extends NodeMessage
case class Accept(rt: RoutingTable) extends NodeMessage

case class GossipEnvelope(from: Address, gossip: Gossip) extends NodeMessage

case class GossipOverview(seen: Map[Address, Boolean] = Map.empty)

case class Gossip(
  overview: GossipOverview = GossipOverview(),
  rt: RoutingTable = RoutingTable()) {

  def seen(address: Address): Gossip = {
    if (overview.seen.contains(address)) this
    else this copy (overview = overview copy (seen = overview.seen + (address -> true)))
  }

}

//Stabilize network usign Gossip protocol
class Stabilizer(system: ActorSystem, router: Router) {
  
  val logger = LoggerFactory.getLogger(classOf[Stabilizer])
  val selfAddress = router.self

  var latestGossip: Gossip = Gossip()
  var latestOverview: GossipOverview = GossipOverview()

  // Create the master
  val master = system.actorOf(Props(new Master).withDeploy(Deploy(scope = RemoteScope(router.self))), name = "stabilizer")
  logger.info("Stabiliser start")  
  
  class Master extends Actor {
    import Status._

    val log = Logging(context.system, this)
    
    override def preStart() = {
      log.debug("Stabilizer actor preStart")
    }
         
    def receive = {
      case Send(to: Address) => {
	println("Send gossip to "+to)
	val gossip = latestGossip	
	context.actorFor(to+"/user/stabilizer") ! GossipEnvelope(selfAddress, gossip)	
      }
      //TODO remove
      case Stabilize => {
	val self = router.getNode
 	//check my successor is down
	val defSucc = router.getSuccessor(self)
	if(defSucc.node_state == Down) context.actorFor(defSucc.uri+"/user/stabilizer") ! Envelope(self, router.currentTable)
	val succ = router.getSuccessor(self, (route: Route) => route.node_state != Down)	
	context.actorFor(succ.uri+"/user/stabilizer") ! Envelope(self, router.currentTable)
      }
      
      //listener
      case Join(from) => {	
	router.add(from)	
	sender ! Accept(router.currentTable)	
      }

      case msg: GossipEnvelope => receiveGossip(msg)

      case Heartbeat(from) => router.receiveHeartbeat(from)

      //TODO remove
      case Envelope(from, rt) => {
        log.debug("Envelope from "+ from)
        //check i am your successor 	
	val self = router.getNode	
	val pre = router.getPredecessor(self, (route: Route) => route.node_state != Down)
	if(from.id == pre.id){
	  sender ! Response(true, router.currentTable)
	}else{
	  sender ! Response(false, router.currentTable)
	}
	router.updateRoutingTable(rt)
      }      
      //TODO remove
      case Response(true, rt) => {        
	//update rt
	router.updateRoutingTable(rt)
      }
      //TODO remove
      case Response(false, rt) => {
	//update rt	
	router.updateRoutingTable(rt)
      }      

    }    
  }  
    
  def join(successor: Route): Boolean = {
    logger.debug("Join forward listener "+successor)
    implicit val timeout = Timeout(5 seconds)
    val future = system.actorFor(successor.uri+"/user/stabilizer") ? Join(router.getNode)
    val result = Await.result(future, timeout.duration)   
    result match {
      case Accept(rt) => {
	logger.debug("join accepted update rt")
	router.updateRoutingTable(rt)	
	true
      }
      case _ => false
    }
  }

  def receiveGossip(envelope: GossipEnvelope): Unit = {
    val from = envelope.from
    val remoteGossip = envelope.gossip
    val localGossip = latestGossip
    println("receiveGossip "+from+" version"+remoteGossip.rt.version)
    if(remoteGossip.rt.version > localGossip.rt.version) router.updateRoutingTable(remoteGossip.rt)
  }

  def gossip(): Unit = {
    println("scheduled gossiping called")
    if(!convergence) {
      val rt = router.currentTable
      val overview = latestOverview
      latestGossip = Gossip(overview, rt)
      gossipToRandomNodeOf(router.currentTable.addresses)
    }
  }

  def convergence: Boolean = {    
    false
  }

  private def selectRandomNode(addresses: IndexedSeq[Address]): Option[Address] =
    if (addresses.isEmpty) None
    else Some(addresses(ThreadLocalRandom.current nextInt addresses.size))

  private def gossipToRandomNodeOf(addresses: IndexedSeq[Address]): Unit = {
    val peer = selectRandomNode(addresses filterNot (_ == selfAddress))    
    peer foreach { address => master ! Send(address) }
  }

}


