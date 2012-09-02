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

object JoinConfig extends BriseisConfig {
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
  }

}

  //事前準備
  //DB初期化
  //データ登録（Handoffが発生するようなデータを登録）
  //新規Join時のテストケース

class JoinSpec extends Specification {
  import JoinConfig._
  var nodes: List[Node] = List.empty
  var STRoutingTable:List[Route] = List.empty

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

  "2 node network" should {
    for(i <- 1 to 2){
      println("vm"+i+" start")
      JoinConfig.app_no="app_"+i
      if (i == 2) JoinConfig.port = JoinConfig.port + 10
      nodes = new Node(JoinConfig) :: nodes
      TimeUnit.SECONDS.sleep(20)
      if (i == 1) {
        val serviceA = new Briseis(JoinConfig, nodes(0))
        serviceA.start()
        "Start nodeA" in {
          //ノードを1台（ノードA）起動して Up になったことを確認
          nodes(0).router.state.toString must contain ("Up")
          //ルーティングテーブルを確認
          serviceA.briseisService.stats().table must contain (nodes(0).router.self.toString)
        }
        "nodeA is running" in {
          //データ登録動作確認
          val wall = new delivery_wall_messages(1000000000999L,1L,2L,3,4,5,6,true,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceA.briseisService.put_delivery_wall_messages("1000000000999",wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
          //データ取得動作確認
          TimeUnit.SECONDS.sleep(5)
          serviceA.briseisService.get_delivery_wall_messages("1000000000999","id = 1000000000999",thrift.ConsistencyLevel.PRIME) must not be ("[]")
        }
      } else {
        val serviceB = new Briseis(JoinConfig, nodes(1))
        serviceB.start()
        "Start nodeB" in {
        //ノードを1台（ノードB）追加して ステータス確認
          nodes(1).router.state.toString must contain ("Joining")
          //Picker起動中もデータ登録動作が正常に実行されること
          val wall = new delivery_wall_messages(1000000009998L,1L,2L,3,4,5,6,true,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceB.briseisService.put_delivery_wall_messages("1000000009998",wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
        }
        "Routing Table has 2 routings and correct order" in {
          //ルーティングテーブルを確認（2台起動かつIDが昇順に並ぶ）
          serviceB.briseisService.stats().table must contain (nodes(1).router.self.toString)
          val singleList:List[Route] = List(nodes(0).router.getNode)
          val newList = singleList :+ nodes(1).router.getNode
          STRoutingTable = newList.sortWith((r1, r2) => SHA1Hasher.compareWith(r1.id, r2.id))
          serviceB.briseisService.stats.table.toString must be_==(STRoutingTable.toString)
        }
        "Picker start and put correct datas" in {
          for (key <- 100000000000000000L to 100000000000000150L) {
            //ノードBがプライマリーノードであると判定されたIDが正しくDBへ登録されているか確認
            val id = SHA1Hasher.genID(key.toString)
            val selfRoute = nodes(1).router.currentTable.table.head
            TimeUnit.SECONDS.sleep(1)
            val query = "id = "+key
            val res = serviceB.briseisService.get_delivery_wall_messages(key.toString,query,thrift.ConsistencyLevel.PRIME)
            val owner = ownerSearch(id, nodes(1))
              if(owner.toString == selfRoute.toString) {
                "This data owner correct" in {
                  res.toString must contain ("nodeB")
                }
              } else {
                "Don't pick data" in {
                  res.toString must not (contain("nodeB"))
                }
              }
            }
          }
        "nodeB is running" in {
          //ノードBが Up になったことを確認
          nodes(1).router.state.toString must contain ("Up")
          //データ登録動作確認
          val wall = new delivery_wall_messages(1000000009999L,1L,2L,3,4,5,6,true,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","")
          serviceB.briseisService.put_delivery_wall_messages("1000000009999",wall,thrift.ConsistencyLevel.PRIME) must be_==(1)
          //データ取得動作確認
          TimeUnit.SECONDS.sleep(1)
          serviceB.briseisService.get_delivery_wall_messages("1000000009999","id = 1000000009999",thrift.ConsistencyLevel.PRIME) must not be ("[]")
        }
      }
    }
    "2 nodes shutdown success" in {
      nodes.size must be_==(2)
      nodes(0).shutdown must not (throwA[Exception])
      nodes(1).shutdown must not (throwA[Exception])
    }
  }
}

// TEST MEMO

  //1．Lookup リーダーノード
  //ノードを1台（ノードC）追加して Joining ステータス確認
  //ノードCで Picker が起動したことを確認
  //ノードBで Marker が起動したことを確認(レプリカ2の場合、削除はない)
  //ノードAで Marker が起動したことを確認(レプリカ2の場合、削除はない)
  //Picker起動中もデータ登録動作が正常に実行されること
  //ノードCが Up になったことを確認
  //ルーティングテーブルを確認（3台起動かつIDが昇順に並ぶ）
  //データ登録動作確認
  //データ取得動作確認

  //2．Lookup リーダーノード以外
  //ノードを1台（ノードC）追加して Joining ステータス確認
  //ノードCで Picker が起動したことを確認
  //ノードBで Marker が起動したことを確認(レプリカ2の場合、削除はない)
  //ノードAで Marker が起動したことを確認(レプリカ2の場合、削除はない)
  //Picker起動中もデータ登録動作が正常に実行されること
  //ノードCが Up になったことを確認
  //ルーティングテーブルを確認（3台起動かつIDが昇順に並ぶ）
  //データ登録動作確認
  //データ取得動作確認

  //再起動の場合は、handoffpickerが起動すること
  //定期的にhandoffpickerが起動すること
