package jp.ne.internavi.sync.hcp.dist

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
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig}

case object Stabilize extends NodeMessage
case class Join(from: Route) extends NodeMessage
case class Heartbeat(from: Address) extends NodeMessage
//local command
case class Envelope(from: Route,  rt: RoutingTable) extends NodeMessage    
case class Response(b: Boolean, rt: RoutingTable) extends NodeMessage
case class Accept(rt: RoutingTable) extends NodeMessage

class Stabilizer(system: ActorSystem, router: Router) {
  
  val logger = LoggerFactory.getLogger(classOf[Stabilizer])

  // Create the master
  val master = system.actorOf(Props(new Master).withDeploy(Deploy(scope = RemoteScope(router.self))), name = "stabilizer")
  var cancellable: Cancellable = null //system.scheduler.schedule(0 milliseconds, 1000 milliseconds, master, Stabilize)  
  
  class Master extends Actor {
    
    val log = Logging(context.system, this)
    
    override def preStart() = {
      log.debug("Stabilizer actor preStart")
    }
         
    def receive = {      
      case Stabilize => {
	val uri = router.getSuccessor.uri
	logger.debug("stabilize to "+uri)
	context.actorFor(uri+"/user/stabilizer") ! Envelope(router.getNode, router.currentTable)	
      }
      case Response(true, rt) => {
        log.debug("response true")
	//update rt
	router.updateRoutingTable(rt)
      }
      case Response(false, rt) => {
	//update rt
	log.debug("response false")
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
	if(from.id == router.getPredecessor.id){
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
  
  def start(): Unit = {
    cancellable = system.scheduler.schedule(0 milliseconds, 
					    1000 milliseconds, 
					    master, 
					    Stabilize)
    println("Stabiliser start")
  }
  
  def cancel(): Unit = cancellable.cancel()
    
}
