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
import net.khonda.eris.config.{Eris => ErisConfig}

case object Stabilize extends NodeMessage
case class Join(from: Route) extends NodeMessage
case class Heartbeat(from: Address) extends NodeMessage
//local command
case class Envelope(from: Route,  rt: RoutingTable) extends NodeMessage
case class Response(b: Boolean, rt: RoutingTable) extends NodeMessage
case class Accept(rt: RoutingTable) extends NodeMessage


//Stabilize network usign Gossip protocol
class Stabilizer(system: ActorSystem, router: Router) {
  
  val logger = LoggerFactory.getLogger(classOf[Stabilizer])

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
      case Stabilize => {
	val self = router.getNode
 	//check my successor is down
	val defSucc = router.getSuccessor(self)
	if(defSucc.node_state == Down) context.actorFor(defSucc.uri+"/user/stabilizer") ! Envelope(self, router.currentTable)
	val succ = router.getSuccessor(self, (route: Route) => route.node_state != Down)	
	context.actorFor(succ.uri+"/user/stabilizer") ! Envelope(self, router.currentTable)
      }
      case Response(true, rt) => {        
	//update rt
	router.updateRoutingTable(rt)
      }
      case Response(false, rt) => {
	//update rt	
	router.updateRoutingTable(rt)
      }      
      //listener
      case Join(from) => {	
	router.add(from)	
	sender ! Accept(router.currentTable)	
      }
      case Envelope(from , rt) => {
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
      case Heartbeat(from) => router.receiveHeartbeat(from)
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

  def gossip(): Unit = {
    println("stabi run gossip")
    // if(convergence) 

  }

  def convergence: Boolean = {
    false
  }
/*
  private def gossipToRandomNodeOf(addresses: IndexedSeq[Address]): Option[Address] = {

  }
*/
}

