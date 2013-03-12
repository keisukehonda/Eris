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

/**
 * INTERNAL API for CRUD.
 */
class NodeDaemon(system: ActorSystem, router: Router) {

  val logger = LoggerFactory.getLogger(classOf[NodeDaemon])
  val selfAddress = router.self

  var latestGossip: Gossip = Gossip()  

  // Create the master
  val master = system.actorOf(Props(new Master).withDeploy(Deploy(scope = RemoteScope(router.self))), name = "core")
  logger.info("Node Daemon start")

  class Master extends Actor {

    def receive = {
      case _ => {
	println("node daemon get message")
      } 
    }

  }

}
