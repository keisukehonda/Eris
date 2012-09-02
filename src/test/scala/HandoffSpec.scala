package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import jp.ne.internavi.sync.hcp.dist._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import jp.ne.internavi.sync.hcp.dist.DistClient

/*
object HandoffConfig extends BriseisConfig {
  //edit
  var app_no = "app_0"
  val hostname = "127.0.0.1"
  var port = 7919
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  val rose_server = ("127.0.0.1", 7819)

  //do not edit  
  val nodeInfo = (app_no, hostname, port)
    
  val server = new BriseisThriftServer with THsHaServer {
  }
}
*/
object nodeAConfig extends BriseisConfig {
  //edit
  val app_no = "app_1"
  val hostname = "127.0.0.1"
  var port = 7919
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  val rose_server = ("127.0.0.1", 7819)

  //do not edit  
  val nodeInfo = (app_no, hostname, port)
    
  val server = new BriseisThriftServer with THsHaServer {
  }

}
object nodeBConfig extends BriseisConfig {
  //edit
  val app_no = "app_2"
  val hostname = "127.0.0.1"
  var port = 7919
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  val rose_server = ("127.0.0.1", 7819)

  //do not edit  
  val nodeInfo = (app_no, hostname, port)
    
  val server = new BriseisThriftServer with THsHaServer {
  }
}

object nodeCConfig extends BriseisConfig {
  //edit
  val app_no = "app_3"
  val hostname = "127.0.0.1"
  var port = 7919
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  val rose_server = ("127.0.0.1", 7819)

  //do not edit  
  val nodeInfo = (app_no, hostname, port)
    
  val server = new BriseisThriftServer with THsHaServer {
  }
}

class HandoffSpec extends Specification {

//nodeA=11111... nodeB=99999... nodeC=55555...
  import nodeAConfig._
  import nodeBConfig._
  import nodeCConfig._

  args(sequential=true)

  val nodeMax: Int = 3
  var startId: Long = 2000000000L
  var endId: Long = 0L

  def StartNode(startApp_no:Int):(Node,Briseis) = {

    var config:BriseisConfig = null
    if (startApp_no == 1) {
      config = nodeAConfig
    }
    if (startApp_no == 2) {
      config = nodeBConfig
    }
    if (startApp_no == 3) {
      config = nodeCConfig
    }
    val node = new Node(config)
    var service: Briseis = new Briseis(config, node)
    TimeUnit.SECONDS.sleep(10)
    (node, service)
  }

  def ownerSearch(id:String, node:Node): Route = {
    val localTable = node.router.currentTable.table
    val head = localTable.head
    val tail = localTable.tail
    if(localTable.size != 1) localSearch(id, head, tail, head) else head
  }

  def localSearch(id: String, self: Route, list: List[Route], top: Route): Route = {
    val next = list.head
    if(SHA1Hasher.isInInterval(id, self.id, next.id)) next else {
      if(list.size==1) top else localSearch(id, list.head, list.tail, top)
    }
  }

  var nodes:List[Node] = List.empty
  var services:List[Briseis] = List.empty

//TOTAL 3ノード
  for (i <- 1 to nodeMax){
    println("START NODE APP#"+i)
    val res = StartNode(i) // res = (Node, Briseis)
    nodes = res._1 :: nodes
    services = res._2 :: services
  }

  val nodeA = nodes(2)
  val nodeB = nodes(1)
  val nodeC = nodes(0)
  val serviceA = services(2)
  val serviceB = services(1)
  val serviceC = services(0)
  val addressA = nodeA.router.self
  val addressB = nodeB.router.self
  val addressC = nodeC.router.self

  "The Handoff action" should {
    "nodeA,B,C Start running" in {
    //0．3ノード稼働確認(nodeA,nodeB,nodeC)
      //正しくデータがput/get出来ること
      val wall1 = new delivery_wall_messages(startId,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceA.briseisService.put_delivery_wall_messages(startId.toString,wall1,thrift.ConsistencyLevel.PRIME) must be_==(1)
      val wall2 = new delivery_wall_messages(startId+1L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceB.briseisService.put_delivery_wall_messages((startId+1L).toString,wall2,thrift.ConsistencyLevel.PRIME) must be_==(1)
      val wall3 = new delivery_wall_messages(startId+2L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceC.briseisService.put_delivery_wall_messages((startId+2L).toString,wall3,thrift.ConsistencyLevel.PRIME) must be_==(1)
      TimeUnit.SECONDS.sleep(3)
      serviceC.briseisService.get_delivery_wall_messages(startId.toString,"id = "+startId,thrift.ConsistencyLevel.PRIME) must not be ("[]")
      serviceA.briseisService.get_delivery_wall_messages((startId+1L).toString,"id = "+(startId+1L),thrift.ConsistencyLevel.PRIME) must not be ("[]")
      serviceB.briseisService.get_delivery_wall_messages((startId+2L).toString,"id = "+(startId+2L),thrift.ConsistencyLevel.PRIME) must not be ("[]")
    }
    //1．1ノードDown
    "nodeB down and put new datas" in {
      startId = startId + 3L
      //ノードを１つDown
      nodeA.down(addressB)
      TimeUnit.SECONDS.sleep(5)
      nodeB.router.getNode.state.toString must be_==("Down")
      //Handoffが発生するデータを登録
      endId = startId + 20L
      for (i <- startId to endId) {
        "Data "+i+" put nodeA success" in {
          val wallA = new delivery_wall_messages(i,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceA.briseisService.put_delivery_wall_messages(i.toString,wallA,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
        "Data "+(i-100L)+" put nodeC success" in {
          val wallC = new delivery_wall_messages(i-100L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceC.briseisService.put_delivery_wall_messages((i-100L).toString,wallC,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
      }
    }
    "nodeB rejoin and pick handoff datas" in {
      //Downしたノードを再Join
      nodeB.join(addressA)
      //handoffpicker起動
      TimeUnit.SECONDS.sleep(30)
      nodeB.router.getNode.state.toString must be_==("Up")
      val connB = new DistClient(nodeBConfig.rose_server._1, nodeBConfig.rose_server._2)
      for (i <- startId to endId) {
        var id = SHA1Hasher.genID(i.toString)
        var owner = ownerSearch(id, nodeB)
        if (nodeB.router.self.toString == owner.uri.toString) {
          "Put Handoff data from nodeA to nodeB success [id = "+i+"]" in {
            connB.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
        id = SHA1Hasher.genID((i-100L).toString)
        owner = ownerSearch(id, nodeB)
        if (nodeB.router.self.toString == owner.uri.toString) {
          "Put Handoff data from nodeC to nodeB success [id = "+(i-100L)+"]" in {
            connB.get_delivery_wall_messages((i-100L).toString,"id = "+(i-100L),thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
      }
    }
    //2．2ノードDown
    "nodeB&nodeC down and put new datas" in {
      //ノードを2つDown
      nodeA.down(addressB)
      TimeUnit.SECONDS.sleep(5)
      nodeB.router.getNode.state.toString must be_==("Down")
      nodeA.down(addressC)
      TimeUnit.SECONDS.sleep(5)
      nodeC.router.getNode.state.toString must be_==("Down")
      //B,CノードともHandoffが発生するデータを登録
      startId = endId + 1L
      endId = startId + 19L
      for (i <- startId to endId) {
        "Data "+i+" put success" in {
          val wallA = new delivery_wall_messages(i,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceA.briseisService.put_delivery_wall_messages(i.toString,wallA,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
      }
      startId = endId + 1L
    }
    "nodeB&nodeC rejoin and pick handoff datas" in {
      //Downしたノードを再Join
      nodeB.join(addressA)
      TimeUnit.SECONDS.sleep(30)
      nodeC.join(addressA)
      TimeUnit.SECONDS.sleep(30)
      for (i <- startId to endId) {
        val id = SHA1Hasher.genID(i.toString)
        val owner = ownerSearch(id, nodeA)
        if (nodeB.router.self.toString == owner.uri.toString) {
          val connB = new DistClient(nodeBConfig.rose_server._1, nodeBConfig.rose_server._2)
          "Put Handoff data from nodeA to nodeB success [id = "+i+"]" in {
            connB.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
        if (nodeC.router.self.toString == owner.uri.toString) {
          val connC = new DistClient(nodeCConfig.rose_server._1, nodeCConfig.rose_server._2)
          "Put Handoff data from nodeA to nodeC success [id = "+i+"]" in {
            connC.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
      }
    }
    "Node status are correct" in {
      nodeA.router.getNode.state.toString must be_==("Up")
      nodeB.router.getNode.state.toString must be_==("Up")
      nodeC.router.getNode.state.toString must be_==("Up")
    }
  }
  //3．2ノードDown（時差あり）
  "The Schedule Handoff action" should {
  //1ノードDownから時間をおいて2ノード目をDown（DownしているノードにもHandoffデータが存在する状況）//
    "nodeB Down and put data success" in {
    //1ノード目（nodeB）Down
      nodeA.down(addressB)
      TimeUnit.SECONDS.sleep(5)
      nodeB.router.getNode.state.toString must be_==("Down")
      //データ登録（Upの2ノードどちらにもHintを登録する）
      endId = startId + 19L
      for (i <- startId to endId) {
        "Data "+i+" put nodeA success" in {
          val wallA = new delivery_wall_messages(i,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceA.briseisService.put_delivery_wall_messages(i.toString,wallA,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
        "Data "+(i-100L)+" put nodeC success" in {
          val wallC = new delivery_wall_messages(i-100L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceC.briseisService.put_delivery_wall_messages((i-100L).toString,wallC,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
      }
      startId = endId + 1L
    }
    "nodeC Down and put data success" in {
    //2ノード目（nodeC）Down
      nodeA.down(addressC)
      TimeUnit.SECONDS.sleep(5)
      nodeC.router.getNode.state.toString must be_==("Down")
      //データ登録
      endId = startId + 19L
      for (i <- startId to endId) {
        "Data "+i+" put nodeA success" in {
          val wallA = new delivery_wall_messages(i,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceA.briseisService.put_delivery_wall_messages(i.toString,wallA,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
      }
    }
    "Restart nodeB and pick Handoff from nodeA" in {
      //停止時間の長い方（nodeB）から再Join
      nodeB.join(addressA)
      TimeUnit.SECONDS.sleep(30)
      //nodeAに登録された Hint の回収を確認
      val connB = new DistClient(nodeBConfig.rose_server._1, nodeBConfig.rose_server._2)
      for (i <- startId to endId) {
        val id = SHA1Hasher.genID(i.toString)
        val owner = ownerSearch(id, nodeA)
        if (nodeB.router.self.toString == owner.uri.toString) {
          "Put Handoff data from nodeA to nodeB success [id = "+i+"]" in {
            connB.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
      }
    }
    "Restart nodeC and nodeB pick Handoff from nodeC" in {
      //nodeB再起動完了後、nodeC（nodeBへのHandoffあり）起動
      nodeC.join(addressA)
      TimeUnit.SECONDS.sleep(30)
      //定期実行している Handoff picker でのデータ回収を確認
      for (i <- startId to endId) {
        var id = SHA1Hasher.genID(i.toString)
        var owner = ownerSearch(id, nodeA)
        if (nodeC.router.self.toString == owner.uri.toString) {
          val connC = new DistClient(nodeCConfig.rose_server._1, nodeCConfig.rose_server._2)
          "Put Handoff data from nodeA to nodeC success [id = "+i+"]" in {
            connC.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
        id = SHA1Hasher.genID((i-100L).toString)
        owner = ownerSearch(id, nodeA)
        if (nodeB.router.self.toString == owner.uri.toString) {
          val connB = new DistClient(nodeBConfig.rose_server._1, nodeBConfig.rose_server._2)
          "Put Handoff data from nodeC to nodeB success [id = "+(i-100L)+"]" in {
            connB.get_delivery_wall_messages((i-100L).toString,"id = "+(i-100L),thrift.ConsistencyLevel.PRIME) must not be ("[]")
          }
        }
      }
      startId = endId + 1L
    }

    "Node status are correct" in {
      nodeA.router.getNode.state.toString must be_==("Up")
      nodeB.router.getNode.state.toString must be_==("Up")
      nodeC.router.getNode.state.toString must be_==("Up")
    }

  }
  "End processing" should {
    "node Shutdown success" in {
      nodes.foreach { node =>
        node.shutdown() must not (throwA[Exception])
      }
      services.foreach { service =>
        service.shutdown() must not (throwA[Exception])
      }
      nodes = List.empty
      nodes must have size(0)
    }
  }
}