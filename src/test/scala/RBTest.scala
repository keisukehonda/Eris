package jp.ne.internavi.sync.hcp.dist

import jp.ne.internavi.sync.hcp.dist.DistClient
import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel

object RBTest{
  def main(host: Array[String]){
    println("Stress Test Start !!")

    var bri = new DistClient("172.27.2.22", 7919)
    var start:Long = 0L
    var end:Long = 0L
    var i:Long = 1L
    var j:Long = 1L
    var tag:Long = 0L

    while(true){
      start = i
      end = start + 999999L
      while(i < end){
        if (i-6*tag == 1) bri = new DistClient("172.27.2.22", 7919)
        if (i-6*tag == 2) bri = new DistClient("172.27.2.23", 7920)
        if (i-6*tag == 3) bri = new DistClient("172.27.2.24", 7921)
        if (i-6*tag == 4) bri = new DistClient("172.27.2.22", 7922)
        if (i-6*tag == 5) bri = new DistClient("172.27.2.23", 7923)
        if (i-6*tag == 6) {
          bri = new DistClient("172.27.2.24", 7924)
          tag = tag + 1L
        }
        val wall = new delivery_wall_messages(i,123456,2,3,4,5,6,true,false,1320238531,"table_name",10123456,11123456,12123456,"tokyo","irft","")
        bri.put_delivery_wall_messages("hoge",wall,ConsistencyLevel.PRIME)

        if(i > 100) {
          bri.get_delivery_wall_messages("hoge",j.toString,ConsistencyLevel.PRIME)
          j = j + 1L
        }
        i = i + 1L
      }
      println("1 loop end. i = "+i)
    }
    println("Stress Test End.")
  }
}