package net.khonda.eris.node

import net.khonda.eris.config.{Eris => ErisConfig}


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


class Node {

}

