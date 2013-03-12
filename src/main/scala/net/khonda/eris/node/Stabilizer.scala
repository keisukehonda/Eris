package net.khonda.eris.node

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import net.khonda.eris._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.collection.immutable.Map

import net.khonda.eris.config.{Eris => ErisConfig}

//Stabilize network using Gossip protocol
class Stabilizer(system: ActorSystem, router: Router) {
  
  val logger = LoggerFactory.getLogger(classOf[Stabilizer])
  val selfAddress = router.self

  var latestGossip: Gossip = Gossip()  

  // Create the master
  val master = system.actorOf(Props(new Master).withDeploy(Deploy(scope = RemoteScope(router.self))), name = "stabilizer")
  logger.info("Node Stabiliser start")
  
  class Master extends Actor {
    import Status._

    val log = Logging(context.system, this)
    
    override def preStart() = {
      log.debug("Stabilizer actor preStart")
    }
         
    def receive = {
      case Send(to: Address, envelope: GossipEnvelope) => {
	println("Send gossip to "+to+" as ")	
	context.actorFor(to+"/user/stabilizer") ! envelope 
      }

      //listener
      case Join(from) => {	
	router.add(from)	
	sender ! Accept(router.currentTable)	
      }

      case msg: GossipEnvelope => receiveGossip(msg)

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
	logger.debug("join accepted update rt and reset gossip info")
	router.updateRoutingTable(rt)	
	//reset overview seen
	val latestOverview = latestGossip.overview 	
	latestGossip = Gossip(latestOverview, router.currentTable) seen selfAddress
	println(latestGossip)	
	true
      }
      case _ => false
    }
  }

  def receiveGossip(envelope: GossipEnvelope): Unit = {
    val from = envelope.from
    val conversation = envelope.conversation
    val remoteGossip = envelope.gossip
    val localGossip = latestGossip

    println("receiveGossip "+from+" version"+remoteGossip.rt.version)
    
    val winningGossip = 
      if (remoteGossip.rt.version < localGossip.rt.version) localGossip
      else remoteGossip

    latestGossip = winningGossip seen selfAddress

    if (winningGossip == remoteGossip) {
      router.updateRoutingTable(remoteGossip.rt)
      if(conversation) oneWayGossipTo(from) //callback at once
    }

  }

  def gossip(): Unit = {
    println("scheduled gossiping called")
    val localGossip = latestGossip
    if (!localGossip.convergence) {      
      gossipToRandomNodeOf(router.currentTable.addresses)
      //gossip to my db
      gossipTo(router.mydb)
    }
  }  

  /**
   * Gossips latest gossip to an address.
   */
  def gossipTo(address: Address): Unit =
    gossipTo(address, GossipEnvelope(selfAddress, latestGossip, conversation = true))

  def oneWayGossipTo(address: Address): Unit =
    gossipTo(address, GossipEnvelope(selfAddress, latestGossip, conversation = false))

  def gossipTo(address: Address, envelope: GossipEnvelope): Unit = 
    if (address != selfAddress) master ! Send(address, envelope)
  
  private def selectRandomNode(addresses: IndexedSeq[Address]): Option[Address] =
    if (addresses.isEmpty) None
    else Some(addresses(ThreadLocalRandom.current nextInt addresses.size))

  private def gossipToRandomNodeOf(addresses: IndexedSeq[Address]): Unit = {
    val peer = selectRandomNode(addresses filterNot (_ == selfAddress))    
    peer foreach { address => gossipTo(address) }
  }
  

}


