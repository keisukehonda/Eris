package chord

import jp.ne.internavi.sync.hcp.dist._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import com.twitter.conversions.time._
import com.twitter.conversions.storage._
import org.specs2.mutable._
import scala.actors.Futures._
import scala.concurrent.ops._
import java.util.concurrent.TimeUnit

object TestConfig extends BriseisConfig {
  //edit
  var app_no = "app_1"
  val hostname = "127.0.0.1"
  var port = 7919
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  val rose_server = ("127.0.0.1", 7819)
 
  //do not edit  
  val nodeInfo = (app_no, hostname, port)
    
  val server = new BriseisThriftServer with THsHaServer {
    port = nodeInfo._3
    timeout = 100.millis
    idleTimeout = 10.seconds
    threadPool.minThreads = 10
    threadPool.maxThreads = 10
  }
}

object LargeClusterMultiJvmNode {

  def main(args: Array[String]){
    println("Large Cluster Speccs start")
    val nodes: List[Node] = List.empty
    for(i <- 1 to 20){
      println("vm"+i+" start")
      TestConfig.app_no="app_"+i
      try {
        new Node(TestConfig) :: nodes
      } catch {
        case e:RuntimeException => {
          TimeUnit.SECONDS.sleep(60)
          try {
            new Node(TestConfig) :: nodes
          } catch {
            case e:RuntimeException => {
              TimeUnit.SECONDS.sleep(60)
              new Node(TestConfig) :: nodes
            }
          }
        }
      }
      TimeUnit.SECONDS.sleep(30)
    }
    println("Create sccess 20 Nodes Large Cluster")
  }
}
