package net.khonda.eris.config

import akka.util.Duration

trait Eris {

  def mode: String
  def app_no: String
  def hostname: String

  def failuredetector_duration: (Duration, Duration)
  def failuredetector_threshold: Double
  def failuredetector_maxSampleSize: Int
  def failuredetector_minStdDeviation: Duration
  def failuredetector_acceptableHeartbeatPause: Duration
  def failuredetector_firstHeartbeatEstimate: Duration

  def getUri(hostname: String, port: Int): String = "akka://ChordSystem-"+port+"@"+hostname+":"+port

}

