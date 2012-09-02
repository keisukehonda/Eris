package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import jp.ne.internavi.sync.hcp.dist._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import com.typesafe.config.ConfigFactory
import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
import java.util.concurrent.TimeUnit
import scala.actors.Futures._
import scala.concurrent.ops._
import com.twitter.conversions.time._
import com.twitter.conversions.storage._

object LeaveConfig extends BriseisConfig {
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

class LeaveSpec extends Specification {
  import LeaveConfig._
  var nodes:List[Node] = List.empty
  var services:List[Briseis] = List.empty
//  var STRoutingTable:List[Route] = List.empty

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

  def StartNode(startApp_no:Int):(Node,Briseis) = {
    import LeaveConfig._
    var config = LeaveConfig
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
    if (startApp_no == 5) {
      config.app_no = "app_5"
      config.port = 7925
      config.rose_server = ("127.0.0.1", 7819)
    }
    val node = new Node(config)
    val service: Briseis = new Briseis(config, node)
    TimeUnit.SECONDS.sleep(10)
    (node, service)
  }

//
  //事前準備
  //DB初期化
  //データ登録（Handoffが発生するようなデータを登録）
  //Leave時のテストケース

//nodeA=11111... nodeB=99999... nodeC=55555... nodeD=ddddd... nodeE=33333...

  for(i <- 1 to 5){
    println("START NODE APP#"+i)
    val res = StartNode(i) // res = (Node, Briseis)
    nodes = res._1 :: nodes
    services = res._2 :: services
  }

  val nodeA = nodes(4)
  val nodeB = nodes(3)
  val nodeC = nodes(2)
  val nodeD = nodes(1)
  val nodeE = nodes(0)
  val serviceA = services(4)
  val serviceB = services(3)
  val serviceC = services(2)
  val serviceD = services(1)
  val serviceE = services(0)
  val addressA = nodeA.router.self
  val addressB = nodeB.router.self
  val addressC = nodeC.router.self
  val addressD = nodeC.router.self
  val addressE = nodeC.router.self

  "When a node is Leaveing" should {
    "The nodeD leave" in {
      nodeA.leave(addressD)
      TimeUnit.SECONDS.sleep(1)
      nodeD.router.getNode.state.toString must be_==("Leaving")
      //Leave picker 起動
      TimeUnit.SECONDS.sleep(5)
      nodeD.router.getNode.state.toString must be_==("Exiting")
      //PRIM=nodeBのデータが（nodeAは新しくnodeBのレプリカ先になる）登録されているかで確認
      val connRose_A = nodeA.broker.getRoseConn(nodeA.router.getNode)
      val id = 10000L//dummy
      connRose_A.get_delivery_wall_messages(id.toString,"id = "+id,thrift.ConsistencyLevel.PRIME).toString must not be_==("[]")

      //
    }
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

// MEMO

  //以下のテストは4ノード以上で実施
  //範囲外データが削除対象（validity = true）となること
  //Leaveの場合、担当範囲の拡大にともないLeavingPickerがpickしたデータを削除対象から外す
  //ルーティングテーブルの範囲とノードステートを確認し範囲外データを計算できること
