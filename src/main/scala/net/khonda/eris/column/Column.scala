package net.khonda.eris.column

import akka.util._
import net.khonda.eris._


case class Column(name: String, value: ByteString, timestamp: Long) extends NodeMessage

case class ColumnFamily(key: Long, name: String, value: List[Column]) extends NodeMessage {

  def newBuilder: Unit = {

  }

  def isEmpty: Boolean = value.isEmpty

  def length: Int = value.length   

}

case class KeySpace(keyspace: String, value: List[ColumnFamily]) extends NodeMessage

object ColumnPath extends NodeMessage {

  def apply(keyspace: String, columnFamily: String): ColumnPath = {
    new ColumnPath(keyspace, columnFamily)
  }

}

class ColumnPath(keyspace: String, columnFamily: String) extends NodeMessage {

  def setColumn(column: Column) = {
  }

}
