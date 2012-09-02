package jp.ne.internavi.sync.hcp.dist

import java.security.MessageDigest
import java.nio.{ByteBuffer, ByteOrder}

object SHA1Hasher extends (String => String) {

  def apply(str: String) = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(str.getBytes)
    md.digest.foldLeft("") { (s, b) => s + "%02x".format(if(b < 0) b + 256 else b) }
  }

  def genID(key: String): String = {
    apply(key)
  }

  def half(str: String) = {
    apply(str).substring(0,20)
  }

  def compare(id1: String, id2: String): Int = {
    val bytes1 = id1.getBytes("UTF-8")
    val bytes2 = id2.getBytes("UTF-8")

    var result = 0
    var break = false			
    for (i <- 0 until bytes1.length if break != true) {
      if( bytes1(i) < bytes2(i) ) {
	result = -1
	break=true
      }else if( bytes1(i) > bytes2(i)) {
	result = 1
	break=true
      }
    }
    result	
  }

  def compareWith(id1: String, id2: String): Boolean = {
    val bytes1 = id1.getBytes("UTF-8")
    val bytes2 = id2.getBytes("UTF-8")
    
    var result = true
    var break = false			
    for (i <- 0 until bytes1.length if break != true) {
      if( bytes1(i) < bytes2(i) ) {
	result = true
	break=true
      }else if( bytes1(i) > bytes2(i)) {
	result = false
	break=true
      }
    }
    result	
  }

  def isInInterval(id: String, from: String, to: String) : Boolean = {
    compare(from,to) match {
      //only one node network
      case 0  => true
      // interval does not cross zero -> compare with both bounds
      case -1 => compare(id,from) > 0 && compare(id,to) < 0
      // interval crosses zero -> split interval at zero
      case 1  => {
	//logger.debug("calculate min and max IDs")
        try{
	  val maxIDBytes = new Array[Byte](40)
          val minIDBytes = new Array[Byte](40)
          for (i <- 0 until maxIDBytes.length) {
            maxIDBytes(i) = 102 //code f
	    minIDBytes(i) = 48  //cde 0
          }
          val max = new String(maxIDBytes, "UTF-8")
          val min = new String(minIDBytes, "UTF-8")
          // check both splitted intervals
          val res = ((compare(from,max) != 0 && compare(id,from) > 0 && compare(id,max) <= 0) || (compare(min,to) != 0 && compare(id,min) >= 0 && compare(id,to) < 0))
      	  res				  				
  	}catch{
  	  case e: Exception => false
	}
      }
    }
  }
  		
}
