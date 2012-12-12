package net.khonda.eris.db

import akka.actor._
import akka.event.Logging
import akka.remote.RemoteScope
import ch.qos.logback._
import net.khonda.eris.node._
import org.slf4j.Logger
import org.slf4j.LoggerFactory


//Stabilize network using Gossip protocol
class Stabilizer(system: ActorSystem, self: Address) {
  
  val logger = LoggerFactory.getLogger(classOf[Stabilizer])

  var latestGossip: Gossip = Gossip()

  // Create the master
  val master = system.actorOf(Props(new Master).withDeploy(Deploy(scope = RemoteScope(self))), name = "stabilizer")
  logger.info("Db stabiliser start")
  
  class Master extends Actor {
 
    val log = Logging(context.system, this)
         
    def receive = {      
      case msg: GossipEnvelope => receiveGossip(msg)     
    }    
  } 

  def receiveGossip(envelope: GossipEnvelope): Unit = {
    val from = envelope.from
    val remoteGossip = envelope.gossip
    val localGossip = latestGossip

    println("receiveGossip "+from+" version"+remoteGossip.rt.version)
    
    val winningGossip = 
      if (remoteGossip.rt.version < localGossip.rt.version) localGossip
      else remoteGossip
    latestGossip = winningGossip
  }

}
