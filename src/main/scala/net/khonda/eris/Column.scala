package net.khonda.eris

case class Column(name: String)

case class ColumnFamily(key: Long, value: Array[Column])

case class KeySpace(keyspace: String, value: Array[ColumnFamily])
