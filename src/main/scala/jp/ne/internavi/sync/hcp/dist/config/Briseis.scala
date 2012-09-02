package jp.ne.internavi.sync.hcp.dist.config


trait BriseisThriftServer extends TServer {
  var name = "briseis"
  var port = 7919
}

trait Briseis {
  def server: BriseisThriftServer
  def app_no: String
  def hostname: String
  def port: Int  
  def lookup: (String, Int)
  def autoJoin: Boolean
  def rose_server: (String, Int)
  
  def getUri(hostname: String, port: Int): String = "akka://ChordSystem-"+port+"@"+hostname+":"+port
  
}
