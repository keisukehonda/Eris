package net.khonda.eris.column

class ColumnFamilyBuilder {

  def clear(): Unit = {
  }
  
  def result(): ColumnFamily = {   
    ColumnFamily(100L, "hoge", List[Column]())
  }

  def length: Int = {
    1
  }

  def putInt(name: String, value: Int): Unit = {
  }

  def putDouble(name: String, value: Double): Unit = {
  }

  def putLong(name: String, value: Long): Unit = {
  }

  def putString(name: String, value: String): Unit = {
  }

}
