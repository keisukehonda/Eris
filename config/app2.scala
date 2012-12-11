import net.khonda.eris.config._
import com.twitter.conversions.time._
import com.twitter.conversions.storage._
import scala.concurrent.duration._

new Eris {
  //edit
  val mode = "node"
  val app_no = "app_2"
  val hostname = "127.0.0.1"
  val port = 7929
  val lookup = ("127.0.0.1", 2519)
  val autoJoin = true
  val db = ("127.0.0.1", 7829)

  val failuredetector_duration = (Duration(3000, "milliseconds"), Duration(4000, "milliseconds"))
  val failuredetector_threshold = 8.0
  val failuredetector_maxSampleSize = 10000
  val failuredetector_minStdDeviation = Duration(100, "milliseconds")
  val failuredetector_acceptableHeartbeatPause = Duration(3000, "milliseconds")
  val failuredetector_firstHeartbeatEstimate = Duration(1000, "milliseconds")
  val stabi_seq = Duration(1000, "milliseconds")

/*
  val hostname = "127.0.0.1"
  val port = 7919  
  val lookup = ("127.0.0.1", 2519)
  val autoJoin = true  
  val replica = 2
  val rose_server = ("127.0.0.1", 7819)
  val failuredetector_duration = (Duration(3000, "milliseconds"), Duration(4000, "milliseconds"))
  val failuredetector_threshold = 8.0
  val failuredetector_maxSampleSize = 10000
  val failuredetector_minStdDeviation = Duration(100, "milliseconds")
  val failuredetector_acceptableHeartbeatPause = Duration(3000, "milliseconds")
  val failuredetector_firstHeartbeatEstimate = Duration(1000, "milliseconds")
  val delmarker_duration = (Duration(3, "minutes"), Duration(1, "minutes"))
  val hopicker_duration = (Duration(1, "minutes"), Duration(1, "minutes"))
 
  //do not edit  
  val picksize = 5
  val nodeInfo = (app_no, hostname, port)
  val stabi_seq = Duration(1000, "milliseconds")    
    
  val server = new BriseisThriftServer with THsHaServer {
    port = nodeInfo._3
    timeout = 100.millis
    idleTimeout = 10.seconds
    threadPool.minThreads = 10
    threadPool.maxThreads = 10
  }  
*/  
}
