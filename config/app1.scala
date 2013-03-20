import net.khonda.eris.config._
import com.twitter.conversions.time._
import com.twitter.conversions.storage._
import scala.concurrent.duration._

new Eris {
  //edit
  val db_mode = false
  val app_no = "app_1"
  val db_no = "db_1"
  val hostname = "127.0.0.1" 
  val lookup = ("127.0.0.1", 2519)
  val autoJoin = true 
  val db_user = "postgres"
  val db_pass = "khonda2565"  

  val failuredetector_duration = (Duration(3000, "milliseconds"), Duration(4000, "milliseconds"))
  val failuredetector_threshold = 8.0
  val failuredetector_maxSampleSize = 10000
  val failuredetector_minStdDeviation = Duration(100, "milliseconds")
  val failuredetector_acceptableHeartbeatPause = Duration(3000, "milliseconds")
  val failuredetector_firstHeartbeatEstimate = Duration(1000, "milliseconds")
  val stabi_seq = Duration(1000, "milliseconds")

}
