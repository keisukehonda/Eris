package net.khonda.eris.node

import akka.actor._

import akka.util.Timeout
import akka.pattern.ask
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import java.lang.Thread._

case class Lookup(uri: String) extends NodeMessage
case class Result(rt: RoutingTable) extends NodeMessage

class LookupProxy(system: ActorSystem, router: Router) {
  
  val logger = LoggerFactory.getLogger(classOf[LookupProxy])

  // Create the master
  val master = system.actorOf(Props(new Master).withDeploy(Deploy(scope = RemoteScope(router.self))), name = "lookup")
  println("LookupProxy start")
  
  class Master extends Actor {

    def receive = {
      case Lookup(uri) => {
	logger.debug("lookup request from " + uri)
	val rt = router.currentTable
	sender ! Result(rt)
      }      
    }  
  }

  def lookup(proxy: String): Option[RoutingTable] = {
    val localaddress = router.self.toString
    implicit val timeout = Timeout(5 seconds)
    val future = system.actorFor(AddressFromURIString(proxy) + "/user/lookup") ? Lookup(localaddress)
    val result = Await.result(future, timeout.duration)
    result match {
      case Result(rt) => Some(rt)
      case _ => None
    }
  }
 
}
