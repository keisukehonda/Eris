package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import jp.ne.internavi.sync.hcp.dist._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import com.typesafe.config.ConfigFactory
import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
import java.util.concurrent.TimeUnit

object ReplicaConfig extends BriseisConfig {
  //edit
  var app_no = "app_1"
  val hostname = "127.0.0.1"
  var port = 7919
  val lookup = ("127.0.0.1", 2551)
  val autoJoin = true
  var rose_server = ("127.0.0.1", 7819)
 
  //do not edit  
  val nodeInfo = (app_no, hostname, port)

  val server = new BriseisThriftServer with THsHaServer {
  }
}

class ReplicaSpec extends Specification {

  args(sequential=true)

  var startId: Long = 5000000000L
  var endId: Long = 0L

//nodeA=11111... nodeB=99999... nodeC=55555... nodeD=ddddd...

  def StartNode(startApp_no:Int):(Node,Briseis) = {
    import ReplicaConfig._
    var config = ReplicaConfig
    if (startApp_no == 1) {
      config.app_no = "app_1"
      config.port = 7919
      config.rose_server = ("127.0.0.1", 7819)
    }
    if (startApp_no == 2) {
      config.app_no = "app_2"
      config.port = 7922
      config.rose_server = ("127.0.0.1", 7819)
    }
    if (startApp_no == 3) {
      config.app_no = "app_3"
      config.port = 7923
      config.rose_server = ("127.0.0.1", 7819)
    }
    if (startApp_no == 4) {
      config.app_no = "app_4"
      config.port = 7924
      config.rose_server = ("127.0.0.1", 7819)
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

  def checkReplicaNodes(id:String, node:Node, nodes:List[Node]):(String, String) = {
    var prim = ownerSearch(id, node)
    var primNode = node
    if (prim.uri == nodes(3).router.self.toString) primNode = nodes(3)
    if (prim.uri == nodes(2).router.self.toString) primNode = nodes(2)
    if (prim.uri == nodes(1).router.self.toString) primNode = nodes(1)
    if (prim.uri == nodes(0).router.self.toString) primNode = nodes(0)
    val successor = primNode.router.getSuccessor.uri
    val predecessor = primNode.router.getPredecessor.uri
    println("PRIM:"+prim.uri+" Suc["+successor+"] Pre["+predecessor+"]")
    (predecessor, successor)
  }

  var nodes:List[Node] = List.empty
  var services:List[Briseis] = List.empty

  for (i <- 1 to 4){
    println("START NODE APP#"+i)
    val res = StartNode(i) // res = (Node, Briseis)
    nodes = res._1 :: nodes
    services = res._2 :: services
  }

  val nodeA = nodes(3)
  val nodeB = nodes(2)
  val nodeC = nodes(1)
  val nodeD = nodes(0)
  val serviceA = services(3)
  val serviceB = services(2)
  val serviceC = services(1)
  val serviceD = services(0)
  val addressA = nodeA.router.self
  val addressB = nodeB.router.self
  val addressC = nodeC.router.self
  val addressD = nodeD.router.self
  val connRose_A = nodeA.broker.getRoseConn(nodeA.router.getNode)
  val connRose_B = nodeB.broker.getRoseConn(nodeB.router.getNode)
  val connRose_C = nodeC.broker.getRoseConn(nodeC.router.getNode)
  val connRose_D = nodeD.broker.getRoseConn(nodeD.router.getNode)

  "The Replica action" should {
  //0．4ノード稼働確認(nodeA,nodeB,nodeC,nodeD)
    "There 4 Nodes put/get data success" in {
    //正しくデータがput/get出来ること
      for (i <- 0L to 2L) {
        var id = startId + (4L * i)
        val wall1 = new delivery_wall_messages(id,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
        serviceA.briseisService.put_delivery_wall_messages(id.toString,wall1,thrift.ConsistencyLevel.PRIME) must be_==(1)
        val wall2 = new delivery_wall_messages(id+1L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
        serviceB.briseisService.put_delivery_wall_messages((id+1L).toString,wall2,thrift.ConsistencyLevel.PRIME) must be_==(1)
        val wall3 = new delivery_wall_messages(id+2L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
        serviceC.briseisService.put_delivery_wall_messages((id+2L).toString,wall3,thrift.ConsistencyLevel.PRIME) must be_==(1)
        val wall4 = new delivery_wall_messages(id+3L,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
        serviceC.briseisService.put_delivery_wall_messages((id+3L).toString,wall4,thrift.ConsistencyLevel.PRIME) must be_==(1)
        TimeUnit.SECONDS.sleep(5)
        serviceD.briseisService.get_delivery_wall_messages(id.toString,"id = "+id,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
        serviceA.briseisService.get_delivery_wall_messages((id+1L).toString,"id = "+(id+1L),thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
        serviceB.briseisService.get_delivery_wall_messages((id+2L).toString,"id = "+(id+2L),thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
        serviceC.briseisService.get_delivery_wall_messages((id+3L).toString,"id = "+(id+3L),thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      }
    }
    "There 4 Nodes replica data success" in {
    //正しくレプリカが登録されること
      endId = startId + 11L
      var connRose = connRose_A
      for (i <- startId to endId) {
        println("id = "+i)
        val id = SHA1Hasher.genID(i.toString)
        val res = checkReplicaNodes(id, nodeA, nodes)
        "Replica (id "+i+") to Predecessor" in {
          if (res._1 == addressA) connRose = connRose_A
          if (res._1 == addressB) connRose = connRose_B
          if (res._1 == addressC) connRose = connRose_C
          if (res._1 == addressD) connRose = connRose_D
          connRose.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
        }
        "Replica (id "+i+") to Successor" in {
          if (res._2 == addressA) connRose = connRose_A
          if (res._2 == addressB) connRose = connRose_B
          if (res._2 == addressC) connRose = connRose_C
          if (res._2 == addressD) connRose = connRose_D
          connRose.get_delivery_wall_messages(i.toString,"id = "+i,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
        }
      }
    }

  //1．1ノードDown
    "nodeB Down Case" in {
    //ノードを1つDown
      nodeA.down(addressB)
      TimeUnit.SECONDS.sleep(10)
      nodeB.router.getNode.state.toString must be_==("Down")
    }
    "new data put to nodeD(Suc:nodeB, Pre:nodeA), replica skip nodeB" in {
    //DownしているノードをSucsesserにもつノードをPrimaryとするデータを登録、Downノードがスキップされること
      //id 5000000013L prim 2554 suc 2552 pre 2551
      val idSucB = 5000000013L
      val wall = new delivery_wall_messages(idSucB,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceA.briseisService.put_delivery_wall_messages(idSucB.toString,wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
      TimeUnit.SECONDS.sleep(3)
      connRose_D.get_delivery_wall_messages(idSucB.toString,"id = "+idSucB,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_A.get_delivery_wall_messages(idSucB.toString,"id = "+idSucB,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_B.get_delivery_wall_messages(idSucB.toString,"id = "+idSucB,thrift.ConsistencyLevel.PRIME).toString must be_==("[]")
    }
    "new data put to nodeC(Suc:nodeA, Pre:nodeB), replica skip nodeB" in {
    //DownしているノードをPredecesserにもつノードをPrimaryとするデータを登録、Downノードがスキップされること
      //id 5000000015L prim 2553 suc 2551 pre 2552
      val idPreB = 5000000015L
      val wall = new delivery_wall_messages(idPreB,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceA.briseisService.put_delivery_wall_messages(idPreB.toString,wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
      TimeUnit.SECONDS.sleep(3)
      connRose_C.get_delivery_wall_messages(idPreB.toString,"id = "+idPreB,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_A.get_delivery_wall_messages(idPreB.toString,"id = "+idPreB,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_B.get_delivery_wall_messages(idPreB.toString,"id = "+idPreB,thrift.ConsistencyLevel.PRIME).toString must be_==("[]")
    }
    "rejoin nodeB, be able to get replica data" in {
    //Downしたノードを再Join
      nodeB.join(addressA)
      TimeUnit.SECONDS.sleep(30)
    //handoffpicker起動、スキップされたレプリカデータが登録されること
      connRose_B.get_delivery_wall_messages("5000000013","id = 5000000013",thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_B.get_delivery_wall_messages("5000000015","id = 5000000015",thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
    }

  //2．2ノードDown
    "nodeB and nodeC Down Case" in {
    //連続するノード2つをDown
      nodeA.down(addressB)
      TimeUnit.SECONDS.sleep(10)
      nodeA.down(addressC)
      TimeUnit.SECONDS.sleep(10)
      nodeB.router.getNode.state.toString must be_==("Down")
      nodeC.router.getNode.state.toString must be_==("Down")
    }
    "new data put to nodeD(Suc:nodeB, Pre:nodeA), replica skip nodeB and nodeC" in {
    //DownしているノードをSucsesserにもつノードをPrimaryとするデータを登録、Downノードがスキップされること
      //id 5000000012L prim 2554 suc 2552 pre 2551
      val idSucBC = 5000000012L
      val wall = new delivery_wall_messages(idSucBC,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceA.briseisService.put_delivery_wall_messages(idSucBC.toString,wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
      TimeUnit.SECONDS.sleep(3)
      connRose_D.get_delivery_wall_messages(idSucBC.toString,"id = "+idSucBC,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_A.get_delivery_wall_messages(idSucBC.toString,"id = "+idSucBC,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_B.get_delivery_wall_messages(idSucBC.toString,"id = "+idSucBC,thrift.ConsistencyLevel.PRIME).toString must be_==("[]")
      connRose_C.get_delivery_wall_messages(idSucBC.toString,"id = "+idSucBC,thrift.ConsistencyLevel.PRIME).toString must be_==("[]")
    }
    "new data put to nodeA(Suc:nodeD, Pre:nodeC), replica skip nodeC and nodeB" in {
    //DownしているノードをPredecesserにもつノードをPrimaryとするデータを登録、Downノードがスキップされること
      //id 5000000014L prim 2551 suc 2554 pre 2553
      val idPreCB = 5000000014L
      val wall = new delivery_wall_messages(idPreCB,1L,2L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
      serviceA.briseisService.put_delivery_wall_messages(idPreCB.toString,wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
      TimeUnit.SECONDS.sleep(3)
      connRose_A.get_delivery_wall_messages(idPreCB.toString,"id = "+idPreCB,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_D.get_delivery_wall_messages(idPreCB.toString,"id = "+idPreCB,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_C.get_delivery_wall_messages(idPreCB.toString,"id = "+idPreCB,thrift.ConsistencyLevel.PRIME).toString must be_==("[]")
      connRose_B.get_delivery_wall_messages(idPreCB.toString,"id = "+idPreCB,thrift.ConsistencyLevel.PRIME).toString must be_==("[]")
    }
    "rejoin nodeB, be able to get replica data" in {
    //Downしたノードを再Join
      nodeB.join(addressA)
      TimeUnit.SECONDS.sleep(30)
      nodeC.join(addressA)
      TimeUnit.SECONDS.sleep(30)
    //handoffpicker起動、スキップされたレプリカデータが登録されること
      connRose_B.get_delivery_wall_messages("5000000012","id = 5000000012",thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
      connRose_C.get_delivery_wall_messages("5000000014","id = 5000000014",thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")
    }
  }
}