package net.khonda.eris

import akka.actor._
import akka.util.Timeout
import akka.pattern.ask
import akka.remote.RemoteScope
import akka.event.Logging
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import net.khonda.eris.node._
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.typesafe.config.ConfigFactory

trait NodeMessage extends Serializable

//INTERNL API
case class Join(from: Route) extends NodeMessage
case class Heartbeat(from: Address) extends NodeMessage
case class Send(to: Address, envelope: GossipEnvelope) extends NodeMessage
case class Accept(rt: RoutingTable) extends NodeMessage
case class GossipEnvelope(from: Address, gossip: Gossip, conversation: Boolean = true) extends NodeMessage

case class GossipOverview(seen: Map[Address, Boolean] = Map.empty)

case class Gossip(
  overview: GossipOverview = GossipOverview(),
  rt: RoutingTable = RoutingTable()) {
  
  def seen(address: Address): Gossip = {
    if (overview.seen.contains(address)) this
    else this copy (overview = overview copy (seen = overview.seen + (address -> true)))
  }
    
  def convergence: Boolean = {    
    val members = rt.addresses
    val seen = overview.seen
    def allMembersInSeen = members.forall(m => seen.contains(m))
    println(overview.seen)
    allMembersInSeen
  }    

}

trait Peer {

  val startTime =  java.util.Calendar.getInstance(new java.util.Locale("ja", "JP", "JP")) //TODO
  val akkaConfig = ConfigFactory.load()

}
