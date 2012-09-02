package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import com.typesafe.config.ConfigFactory
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import jp.ne.internavi.sync.hcp.dist._
import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
import jp.ne.internavi.sync.hcp.dist.DistClient
import java.util.concurrent.TimeUnit
import akka.util.duration._
import akka.util.Timeout
import com.twitter.util.Eval

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object LookupSpecConfig extends BriseisConfig {
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
    timeout = com.twitter.util.Duration(100, java.util.concurrent.TimeUnit.MILLISECONDS)
    idleTimeout = com.twitter.util.Duration(10, java.util.concurrent.TimeUnit.SECONDS)
    threadPool.minThreads = 10
    threadPool.maxThreads = 10
  }
}

class LookupSpec extends Specification {
  import LookupSpecConfig._
  val logger = LoggerFactory.getLogger(classOf[LookupSpec])
  val nodeMax: Int = 4
  val startId: Long = 1000000000000L
  val endId: Long = 1000000000100L

  def StartNode(startApp_no:String):Node ={
    import LookupSpecConfig._
    var config = LookupSpecConfig
    config.app_no = startApp_no
    val port = ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
    val node = new Node(config)
    var service: Briseis = new Briseis(config, node)
    TimeUnit.SECONDS.sleep(10)
    node
  }

  def ownerSearch(id:String, node:Node): Route = {
    val localTable = node.router.currentTable.table
    val head = localTable.head
    val tail = localTable.tail
    if(localTable.size != 1) localSearch(id, head, tail, head) else head //1ノードでなければ、担当ノードを探しにいく。
  }

  def localSearch(id: String, self: Route, list: List[Route], top: Route): Route = {
    val next = list.head
    if(SHA1Hasher.isInInterval(id, self.id, next.id)) next else {
      if(list.size==1) top else localSearch(id, list.head, list.tail, top)
    }
  }

  var nodes:List[Node] = List.empty

  for (i <- 1 to nodeMax){
    logger.debug("START NODE APP#"+i)
    var app_no:String = "app_"+i
    nodes = StartNode(app_no) :: nodes
  }

  val bri = new DistClient(LookupSpecConfig.rose_server._1,LookupSpecConfig.rose_server._2)
  var key: Long = 0L
  var id:String = null
  var j: Int = 0
  var matchWord:String = null
  var wall = new delivery_wall_messages(key,1,2,3,4,5,6,true,false,1320238531,"table_name",10,11,12,"tokyo","irft","")

// MatchWord into Wall data and Wall put
  for (key <- startId to endId) {
    TimeUnit.MILLISECONDS.sleep(500)
    id = SHA1Hasher.genID(key.toString)
    val owner = ownerSearch(id, nodes(0))
    j = 0
    while (j < nodes.size) {
      if (nodes(j).router.self.toString == owner.uri.toString) {
        logger.debug("key = "+key.toString+" : nodeSelfUri = "+nodes(j).router.self+" : ownerUri = "+owner.uri)
        matchWord = "OwnerNodeUrl="+owner.uri
        wall = new delivery_wall_messages(key,1,2,3,4,5,6,true,false,1320238531,"table_name",10,11,12,matchWord,"irft.","")
        j = nodes.size + 1
      } else j = j +1
    }
    bri.put_delivery_wall_messages(key.toString,wall,thrift.ConsistencyLevel.PRIME)
  }

  "Lookup " should {
    "data put collect owner" in {
      for (key <- startId to endId) {
        TimeUnit.MILLISECONDS.sleep(50)
        id = SHA1Hasher.genID(key.toString)
        val owner = ownerSearch(id, nodes(0))
        j = 0
        while (j < nodes.size) {
          if (nodes(j).router.self.toString == owner.uri.toString) {
            matchWord = "OwnerNodeUrl="+owner.uri
            j = nodes.size + 1
          } else j = j +1
        }
      val query = "id = "+key.toString
      val res = bri.get_delivery_wall_messages(key.toString,query,ConsistencyLevel.PRIME).toString
      logger.debug("key = "+key.toString+" : DATA = "+res)
      res must contain(matchWord)
      }
      nodes.foreach { node =>
        node.shutdown() must not (throwA[Exception])
      }
    nodes.size must be_==(nodeMax)
    }
  }
}