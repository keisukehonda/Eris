package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import com.twitter.util.Eval
import com.typesafe.config.ConfigFactory
import java.io.File
import java.util.concurrent.TimeUnit
import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import scala.actors.Futures._
import akka.remote.RemoteScope
import akka.pattern.ask

object TestConfig extends BriseisConfig {
  //edit
  var app_no = "app_0"
  val hostname = "127.0.0.1"
  var port = 7918
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  val rose_server = ("172.27.2.22", 7819)
 
  //do not edit
  val nodeInfo = (app_no, hostname, port)

  val server = new BriseisThriftServer with THsHaServer {
    port = nodeInfo._3
    timeout = com.twitter.util.Duration(100, java.util.concurrent.TimeUnit.MILLISECONDS)//100.millis
    idleTimeout = com.twitter.util.Duration(10, java.util.concurrent.TimeUnit.SECONDS)//10.seconds
    threadPool.minThreads = 10
    threadPool.maxThreads = 10
  }
}

class StabilizerSpec extends Specification {
  import TestConfig._
  args(sequential=true)

  val akkaConfig = ConfigFactory.load()

  var configA = TestConfig
  configA.app_no = "app_1"
  val portA = ConfigFactory.load().getConfig(configA.app_no).getInt("akka.remote.netty.port")
  val nodeA = new Node(configA)
  var serviceA: Briseis = new Briseis(configA, nodeA)

  var configB = TestConfig
  configB.app_no = "app_2"
  val portB = ConfigFactory.load().getConfig(configB.app_no).getInt("akka.remote.netty.port")
  val nodeB = new Node(configB)
  var serviceB: Briseis = new Briseis(configB, nodeB)

  var configC = TestConfig
  configC.app_no="app_3"
  val portC = ConfigFactory.load().getConfig(configC.app_no).getInt("akka.remote.netty.port")
  val nodeC = new Node(configB)
  var serviceC: Briseis = new Briseis(configC, nodeC)

  "Stabilizer " should {
    "Stabilizer start success" in {
      serviceA.start() must not (throwA[Exception])
      TimeUnit.SECONDS.sleep(10)
      serviceB.start() must not (throwA[Exception])
      TimeUnit.SECONDS.sleep(10)
      serviceC.start() must not (throwA[Exception])
      TimeUnit.SECONDS.sleep(10)
    }
    "correct Current RoutingTable" in {
      nodeA.router.currentTable must be_==(nodeC.router.currentTable)
      nodeB.router.currentTable must be_==(nodeA.router.currentTable)
      nodeC.router.currentTable must be_==(nodeB.router.currentTable)
    }
    "correct stabilize target" in {
      nodeA.router.getSuccessor.uri must be_==(nodeB.router.self.toString)
      nodeB.router.getSuccessor.uri must be_==(nodeC.router.self.toString)
      nodeC.router.getSuccessor.uri must be_==(nodeA.router.self.toString)
    }
    "Briseis stop success" in {
      nodeA.shutdown() must not (throwA[Exception])
      nodeB.shutdown() must not (throwA[Exception])
      nodeC.shutdown() must not (throwA[Exception])
    }
  }
}
