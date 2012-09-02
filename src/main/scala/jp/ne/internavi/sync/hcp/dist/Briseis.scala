package jp.ne.internavi.sync.hcp.dist

import akka.actor._
import akka.routing.RoundRobinRouter
import akka.util.Duration
import akka.util.duration._
import com.twitter.ostrich.admin.Service
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig}

class Briseis(config: BriseisConfig, node: Node) extends Service {
    
  val briseisService = new BriseisService(config, node)
  
  lazy val briseisThriftServer = {
    val processor = new thrift.Rowz.Processor(briseisService)
    config.server(processor)
  }
  
  def start() {
    new Thread(new Runnable { def run() { briseisThriftServer.serve() } }, "RowzServerThread").start()
  }
  
  def shutdown() {
    
  }

}
