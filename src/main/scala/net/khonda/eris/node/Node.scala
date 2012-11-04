package net.khonda.eris.node

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

case class RoutingTable(version: Long,
			table: List[Route],
			unreachable: Set[Route]) extends NodeMessage


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
    val latest = RoutingTable(0L, List.empty, Set.empty)
    new AtomicReference[State](State(latest))
  }

  lazy val self: Address = {
    val port = ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
    val local = config.getUri(config.hostname, port)
    AddressFromURIString(local)
  }

  def currentTable: RoutingTable = state.get.rt
  
}
