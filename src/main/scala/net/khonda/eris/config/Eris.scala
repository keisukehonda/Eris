package net.khonda.eris.config

import scala.concurrent.duration.FiniteDuration

trait Eris {

  def mode: String
  def app_no: String
  def hostname: String
  def port: Int  
  def lookup: (String, Int)
  def autoJoin: Boolean
  def db: (String, Int)

  def failuredetector_duration: (FiniteDuration, FiniteDuration)
  def failuredetector_threshold: Double
  def failuredetector_maxSampleSize: Int
  def failuredetector_minStdDeviation: FiniteDuration
  def failuredetector_acceptableHeartbeatPause: FiniteDuration
  def failuredetector_firstHeartbeatEstimate: FiniteDuration
  def stabi_seq: FiniteDuration

  def getUri(hostname: String, port: Int): String = "akka://ChordSystem-"+port+"@"+hostname+":"+port

}

