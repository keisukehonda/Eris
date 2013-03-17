package net.khonda.eris.column

class ColumnFamilyBuilder {

  def clear(): Unit = {
  }
  
  def result(): ColumnFamily = {   
    ColumnFamily(100L, "hoge", List[Column]())
  }

  def putDouble(x: Double): Unit = {
  }

}
