package net.khonda.eris.config

import scala.concurrent.duration.FiniteDuration

trait Eris {

  def db_mode: Boolean
  def app_no: String
  def db_no: String
  def hostname: String
  def port: Int  
  def lookup: (String, Int)
  def autoJoin: Boolean
  def db: (String, Int) //thrift port

  def failuredetector_duration: (FiniteDuration, FiniteDuration)
  def failuredetector_threshold: Double
  def failuredetector_maxSampleSize: Int
  def failuredetector_minStdDeviation: FiniteDuration
  def failuredetector_acceptableHeartbeatPause: FiniteDuration
  def failuredetector_firstHeartbeatEstimate: FiniteDuration
  def stabi_seq: FiniteDuration

  def getUri(hostname: String, port: Int): String = "akka://ChordSystem-"+port+"@"+hostname+":"+port

}

object app1 extends Eris {
  import net.khonda.eris.config._
  import com.twitter.conversions.time._
  import com.twitter.conversions.storage._
  import scala.concurrent.duration._

  //edit
  val db_mode = false
  val app_no = "app_1"
  val db_no = "db_1"
  val hostname = "127.0.0.1"
  val port = 7919
  val lookup = ("127.0.0.1", 2519)
  val autoJoin = true
  val db = ("127.0.0.1", 7819)

  val failuredetector_duration = (Duration(3000, "milliseconds"), Duration(4000, "milliseconds"))
  val failuredetector_threshold = 8.0
  val failuredetector_maxSampleSize = 10000
  val failuredetector_minStdDeviation = Duration(100, "milliseconds")
  val failuredetector_acceptableHeartbeatPause = Duration(3000, "milliseconds")
  val failuredetector_firstHeartbeatEstimate = Duration(1000, "milliseconds")
  val stabi_seq = Duration(1000, "milliseconds")
}

object app4 extends Eris {
  import net.khonda.eris.config._
  import com.twitter.conversions.time._
  import com.twitter.conversions.storage._
  import scala.concurrent.duration._

  //edit
  val db_mode = true
  val app_no = "app_1"
  val db_no = "db_1"
  val hostname = "127.0.0.1"
  val port = 7919
  val lookup = ("127.0.0.1", 2519)
  val autoJoin = true
  val db = ("127.0.0.1", 7819)

  val failuredetector_duration = (Duration(3000, "milliseconds"), Duration(4000, "milliseconds"))
  val failuredetector_threshold = 8.0
  val failuredetector_maxSampleSize = 10000
  val failuredetector_minStdDeviation = Duration(100, "milliseconds")
  val failuredetector_acceptableHeartbeatPause = Duration(3000, "milliseconds")
  val failuredetector_firstHeartbeatEstimate = Duration(1000, "milliseconds")
  val stabi_seq = Duration(1000, "milliseconds")
}

object app2 extends Eris {
  import net.khonda.eris.config._
  import com.twitter.conversions.time._
  import com.twitter.conversions.storage._
  import scala.concurrent.duration._
  //edit
  val db_mode = false
  val app_no = "app_2"
  val db_no = "db_2"
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
}


object app3 extends Eris { 
  import net.khonda.eris.config._
  import com.twitter.conversions.time._
  import com.twitter.conversions.storage._
  import scala.concurrent.duration._
  //edit
  val db_mode = false
  val app_no = "app_3"
  val db_no = "db_3"
  val hostname = "127.0.0.1"
  val port = 7939
  val lookup = ("127.0.0.1", 2519)
  val autoJoin = true
  val db = ("127.0.0.1", 7839)

  val failuredetector_duration = (Duration(3000, "milliseconds"), Duration(4000, "milliseconds"))
  val failuredetector_threshold = 8.0
  val failuredetector_maxSampleSize = 10000
  val failuredetector_minStdDeviation = Duration(100, "milliseconds")
  val failuredetector_acceptableHeartbeatPause = Duration(3000, "milliseconds")
  val failuredetector_firstHeartbeatEstimate = Duration(1000, "milliseconds")
  val stabi_seq = Duration(1000, "milliseconds")
}
