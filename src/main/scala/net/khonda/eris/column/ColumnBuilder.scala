package net.khonda.eris.column

class ColumnBuilder {

  def clear(): Unit = {
  }
  
  def result(): ColumnFamily = {   
    ColumnFamily(100L, "hoge", new Array[Column](10))
  }

}
