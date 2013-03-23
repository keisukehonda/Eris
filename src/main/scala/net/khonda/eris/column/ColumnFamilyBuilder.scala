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

  def putByte(array: Array[Byte]): Unit = {
  }

  def putInt(name: String, value: Int): Unit = {
  }

  def putFloat(name: String, value: Float): Unit = {
  }

  def putLong(name: String, value: Long): Unit = {
  }
  
  def putDouble(name: String, value: Double): Unit = {
  }  

  def putString(name: String, value: String): Unit = {
  }

}
