package net.khonda.eris.column

class ColumnFamilyBuilder {

  def clear(): Unit = {
  }
  
  def result(): ColumnFamily = {   
    ColumnFamily(100L, "hoge", List[Column]())
  }

  def putDouble(name: String, value: Double): Unit = {
  }

  def putLong(name: String, value: Long): Unit = {
  }

  def putString(name: String, value: String): Unit = {
  }

}
