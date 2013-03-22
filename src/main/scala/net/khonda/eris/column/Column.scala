package net.khonda.eris.column

import akka.util._
import net.khonda.eris._

object ConsistencyLevel extends Enumeration {				
  val ZERO, ONE, QUORUM, ALL = Value
}

case class Column(name: String, value: ByteString, timestamp: Long) extends NodeMessage

case class ColumnFamily(key: Long, name: String, value: List[Column]) extends NodeMessage {

  def newBuilder: Unit = {

  }

  def isEmpty: Boolean = value.isEmpty

  def length: Int = value.length
   

}

case class KeySpace(keyspace: String, value: List[ColumnFamily]) extends NodeMessage
