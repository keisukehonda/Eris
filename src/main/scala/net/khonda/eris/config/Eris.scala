package net.khonda.eris.config

import scala.concurrent.duration.FiniteDuration


trait Eris {

  def db_mode: Boolean
  def app_no: String
  def db_no: String  
  def lookup: (String, Int)
  def autoJoin: Boolean 
  def db_user: String
  def db_pass: String

  def failuredetector_duration: (FiniteDuration, FiniteDuration)
  def failuredetector_threshold: Double
  def failuredetector_maxSampleSize: Int
  def failuredetector_minStdDeviation: FiniteDuration
  def failuredetector_acceptableHeartbeatPause: FiniteDuration
  def failuredetector_firstHeartbeatEstimate: FiniteDuration
  def stabi_seq: FiniteDuration

}
