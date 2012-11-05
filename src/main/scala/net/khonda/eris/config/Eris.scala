package net.khonda.eris.config

import akka.util.Duration

trait Eris {

  def mode: String
  def app_no: String
  def hostname: String
  def port: Int  
  def lookup: (String, Int)
  def autoJoin: Boolean
  def db: (String, Int)

  def failuredetector_duration: (Duration, Duration)
  def failuredetector_threshold: Double
  def failuredetector_maxSampleSize: Int
  def failuredetector_minStdDeviation: Duration
  def failuredetector_acceptableHeartbeatPause: Duration
  def failuredetector_firstHeartbeatEstimate: Duration
  def stabi_seq: Duration

  def getUri(hostname: String, port: Int): String = "akka://ChordSystem-"+port+"@"+hostname+":"+port

}

