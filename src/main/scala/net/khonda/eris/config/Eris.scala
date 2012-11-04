package net.khonda.eris.config


trait Eris {

  def mode: String
  def app_no: String
  def hostname: String

  def getUri(hostname: String, port: Int): String = "akka://ChordSystem-"+port+"@"+hostname+":"+port

}

