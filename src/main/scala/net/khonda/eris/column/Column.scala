package net.khonda.eris

import akka.util._

object ConsistencyLevel extends Enumeration {				
  val ZERO, ONE, QUORUM, ALL = Value
}

case class Column(name: String, value: ByteString, timestamp: Long) extends NodeMessage

case class ColumnFamily(key: Long, name: String, value: Array[Column]) extends NodeMessage

case class KeySpace(keyspace: String, value: Array[ColumnFamily]) extends NodeMessage
