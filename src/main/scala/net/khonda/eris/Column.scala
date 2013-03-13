package net.khonda.eris

import akka.util._

case class Column(name: String, value: ByteString, timestamp: Long) extends NodeMessage

case class ColumnFamily(key: Long, value: Array[Column]) extends NodeMessage

case class KeySpace(keyspace: String, value: Array[ColumnFamily]) extends NodeMessage
