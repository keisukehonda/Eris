package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import com.typesafe.config.ConfigFactory
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import java.util.concurrent.TimeUnit

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ClusterSpecConfig extends BriseisConfig {
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

class ClusterSpec extends Specification {
  args(sequential=true)
  val logger = LoggerFactory.getLogger(classOf[ClusterSpec])

  def StartNode(appNumber:Int):Node = {
    import ClusterSpecConfig._
    var config = ClusterSpecConfig
    var startApp_no:String = "app_"+appNumber
    config.app_no = startApp_no
    config.port = config.port + 1 //ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
    val node = new Node(config)
    var service: Briseis = new Briseis(config, node)
    TimeUnit.SECONDS.sleep(10)
    node
  }

  def StatusCheck(node:Node):String = {
    TimeUnit.SECONDS.sleep(5)
    //node.router.currentTable.toString
    node.router.getNode.state.toString
  }

  "A Briseis Cluster" should {
    var nodes:List[Node] = List.empty
    var nodeMax: Int = 0

    "Nomal Node status change success" in {
      nodeMax = 6
    //ノード全台起動
      for (i <- 1 to nodeMax){
        logger.debug("START NODE APP#"+i)
        nodes = StartNode(i) :: nodes
      }
      val add_app1 = nodes(0).router.self //2551
      val add_app2 = nodes(1).router.self //2552
      val add_app3 = nodes(2).router.self //2553
      val add_app4 = nodes(3).router.self //2554
      val add_app5 = nodes(4).router.self //2555
      val add_app6 = nodes(5).router.self //2556

    //ノードを１つdown
      nodes(1).down(add_app2)
      StatusCheck(nodes(1)) must be_==("Down")
    //downしたノードをjoin（lookup リーダーノード）
      nodes(1).join(add_app1)
      StatusCheck(nodes(1)) must be_==("Joining")
    //downしたノードをjoin（lookup リーダー以外のノード）
      nodes(1).down(add_app2)
      StatusCheck(nodes(1)) must be_==("Down")
      nodes(1).join(add_app1)
      StatusCheck(nodes(1)) must be_==("Joining")
    //ノードを１つremove
      nodes(1).remove(add_app2)
      StatusCheck(nodes(1)) must be_==("Removed")
    //ノードを１つdown後remove
      nodes(2).down(add_app3)
      StatusCheck(nodes(2)) must be_==("Down")
      nodes(2).remove(add_app3)
      StatusCheck(nodes(2)) must be_==("Removed")
    //ノードを１つleave
      nodes(3).leave(add_app4)
      StatusCheck(nodes(3)) must be_==("Leaving")
      StatusCheck(nodes(3)) must be_==("Exiting")
    //ノードを１つleave後remove
      nodes(3).remove(add_app4)
      StatusCheck(nodes(3)) must be_==("Removed")
    //ノードを１つプロセスごと停止
      //nodes(4).shutdown()
    //ノードを１つleave後、プロセスごと停止
      //nodes(5).leave(add_app6)
      //StatusCheck(nodes(5)) must be_==("Leaving")
      //StatusCheck(nodes(5)) must be_==("Exiting")
      //nodes(5).shutdown()
    //終了
      nodes.size must be_==(6)
    }//in
  //ノード終了
    nodes.foreach{ node => node.shutdown() }

    "Nomal Nodes status change success" in {
    //ノード全台再起動
      nodes = List.empty
      val startAppNumber = nodeMax + 1
      nodeMax = 8 + startAppNumber
      TimeUnit.SECONDS.sleep(5)
      for (i <- startAppNumber to nodeMax){
        logger.debug("START NODE APP#"+i)
        nodes = StartNode(i) :: nodes
      }
      val add_app1 = nodes(0).router.self //2557
      val add_app2 = nodes(1).router.self //2558
      val add_app3 = nodes(2).router.self //2559
      val add_app4 = nodes(3).router.self //2560
      val add_app5 = nodes(4).router.self //2561
      val add_app6 = nodes(5).router.self //2562
      val add_app7 = nodes(6).router.self //2563
      val add_app8 = nodes(7).router.self //2564
      val add_app9 = nodes(8).router.self //2565

    //ノードを２つ以上down
      nodes(1).down(add_app2)
      nodes(2).down(add_app3)
      StatusCheck(nodes(1)) must be_==("Down")
      StatusCheck(nodes(2)) must be_==("Down")
    //downしたノードをjoin（lookup リーダーノード）
      nodes(1).join(add_app1)
      nodes(2).join(add_app1)
      StatusCheck(nodes(1)) must be_==("Joining")
      StatusCheck(nodes(2)) must be_==("Joining")
    //downしたノードをjoin（lookup リーダー以外のノード）
      nodes(1).down(add_app2)
      nodes(2).down(add_app3)
      StatusCheck(nodes(1)) must be_==("Down")
      StatusCheck(nodes(2)) must be_==("Down")
      nodes(1).join(add_app8)
      nodes(2).join(add_app9)
      StatusCheck(nodes(1)) must be_==("Joining")
      StatusCheck(nodes(2)) must be_==("Joining")
    //ノードを２つ以上remove
      nodes(1).remove(add_app2)
      nodes(2).remove(add_app3)
      StatusCheck(nodes(1)) must be_==("Removed")
      StatusCheck(nodes(2)) must be_==("Removed")
    //ノードを２つ以上down後remove
      nodes(3).down(add_app4)
      nodes(4).down(add_app5)
      StatusCheck(nodes(3)) must be_==("Down")
      StatusCheck(nodes(4)) must be_==("Down")
      nodes(3).remove(add_app4)
      nodes(4).remove(add_app5)
      StatusCheck(nodes(3)) must be_==("Removed")
      StatusCheck(nodes(4)) must be_==("Removed")
    //ノードを２つ以上leave
      nodes(5).leave(add_app6)
      nodes(6).leave(add_app7)
      //StatusCheck(nodes(5)) must be_==("Leaving")
      //StatusCheck(nodes(6)) must be_==("Leaving")
      StatusCheck(nodes(5)) must be_==("Exiting")
      StatusCheck(nodes(6)) must be_==("Exiting")
    //ノードを２つ以上leave後remove
      nodes(5).remove(add_app6)
      nodes(6).remove(add_app7)
      StatusCheck(nodes(5)) must be_==("Removed")
      StatusCheck(nodes(6)) must be_==("Removed")
    //ノードを２つ以上プロセスごと停止
      //nodes(7).shutdown()
      //nodes(8).shutdown()
      //StatusCheck(nodes(7)) must be_==("Down")
      //StatusCheck(nodes(8)) must be_==("Down")
    //終了
      nodes.size must be_==(9)
    }//in
  //ノード終了
    nodes.foreach{ node => node.shutdown() }

    "Nomal All Nodes Status change success" in {
      //ノード全台再起動
      nodes = List.empty
      val startAppNumber = nodeMax + 1
      nodeMax = 3 + startAppNumber
      TimeUnit.SECONDS.sleep(5)
      for (i <- startAppNumber to nodeMax){
        logger.debug("START NODE APP#"+i)
        nodes = StartNode(i) :: nodes
      }
      val add_app1 = nodes(0).router.self //2566
      val add_app2 = nodes(1).router.self //2567
      val add_app3 = nodes(2).router.self //2568
      val add_app4 = nodes(3).router.self //2569

      //リーダーノード以外のノードを全てdown
      nodes(1).down(add_app2)
      nodes(2).down(add_app3)
      nodes(3).down(add_app4)
      StatusCheck(nodes(1)) must be_==("Down")
      StatusCheck(nodes(2)) must be_==("Down")
      StatusCheck(nodes(3)) must be_==("Down")

      //downした全てのノードをjoin
      nodes(1).join(add_app1)
      nodes(2).join(add_app1)
      nodes(3).join(add_app1)
      StatusCheck(nodes(1)) must be_==("Joining")
      StatusCheck(nodes(2)) must be_==("Joining")
      StatusCheck(nodes(3)) must be_==("Joining")

      //リーダーノード以外のノードを全てleave
      nodes(1).leave(add_app2)
      nodes(2).leave(add_app3)
      nodes(3).leave(add_app4)
      StatusCheck(nodes(1)) must be_==("Leaving")
      StatusCheck(nodes(2)) must be_==("Leaving")
      StatusCheck(nodes(3)) must be_==("Leaving")
      StatusCheck(nodes(1)) must be_==("Exiting")
      StatusCheck(nodes(2)) must be_==("Exiting")
      StatusCheck(nodes(3)) must be_==("Exiting")

      //リーダーノード以外のノードを全てremove
      nodes(1).remove(add_app2)
      nodes(2).remove(add_app3)
      nodes(3).remove(add_app4)
      StatusCheck(nodes(1)) must be_==("Removed")
      StatusCheck(nodes(2)) must be_==("Removed")
      StatusCheck(nodes(3)) must be_==("Removed")

      //リーダーノード以外のノードを全てプロセスごと停止
    //再起動
      //nodes(1).shutdown()
      //nodes(2).shutdown()
      //nodes(3).shutdown()
      //リーダーノード以外のノードを全てleave後、プロセスごと停止
    //再起動
      //nodes(1).leave()
      //nodes(2).leave()
      //nodes(3).leave()
      //nodes(1).shutdown()
      //nodes(2).shutdown()
      //nodes(3).shutdown()
    //終了
      nodes.size must be_==(4)
    }//in
  //ノード終了
    nodes.foreach{ node => node.shutdown() }

    "Leader Node status change success" in {
      /* リーダーノードを対象に実施 */
    //ノード全台再起動
      nodes = List.empty
      val startAppNumber = nodeMax + 1
      nodeMax = 3 + startAppNumber
      TimeUnit.SECONDS.sleep(5)
      for (i <- startAppNumber to nodeMax){
        logger.debug("START NODE APP#"+i)
        nodes = StartNode(i) :: nodes
      }
      val add_app1 = nodes(0).router.self //2570
      val add_app2 = nodes(1).router.self //2571
      val add_app3 = nodes(2).router.self //2572
      val add_app4 = nodes(3).router.self //2573

    //ノードをdown
      nodes(0).down(add_app1)
      StatusCheck(nodes(0)) must be_==("Down")
      //downしたノードをjoin
      nodes(0).join(add_app2)
      StatusCheck(nodes(0)) must be_==("Joining")
    //ノードをremove
      nodes(0).remove(add_app1)
      StatusCheck(nodes(0)) must be_==("Removed")
    //再起動
    //ノードをdown後remove
      //nodes(0).down()
      //nodes(0).remove()
    //再起動
    //ノードをleave
      //nodes(0).leave()
    //ノードをleave後remove
      //nodes(0).remove()
    //再起動
    //ノードをプロセスごと停止
      //nodes(0).suhtdown()
    //再起動
    //ノードをleave後プロセスごと停止
      //nodes(0).leave()
      //nodes(0).shutdown()
    //終了
      nodes.size must be_==(4)
    }//in
  //ノード終了
    nodes.foreach{ node => node.shutdown() }

  //コンパイルエラー回避用
    "Test success" in {
      1 must be_==(1)
    }
  }// should

  /* 障害連続発生シナリオ - リーダーノードは自動的に先頭ノードが担当するようになったので以下不要？ - */
  //リーダーノードをdown後、１つ以上のノードをdown
  //リーダーノードをdown後、downしたノードをjoin
  //リーダーノードをdown後、１つ以上のノードをleave
  //リーダーノードをdown後、１つ以上のノードをremove
  //リーダーノードをdown後、１つ以上のノードをleaveし、downする
  //リーダーノードをdown後、１つ以上のノードをleaveし、removeする
  //リーダーノードをdown後、１つ以上のノードをプロセスごと停止
  //リーダーノードをdown後、１つ以上のノードをleaveし、プロセスごと停止
  //リーダーノードをdown後、１つ以上のノードをdownし、リーダーノードをjoin
  //リーダーノードをdown後、１つ以上のノードをleaveし、リーダーノードをjoin
  //リーダーノードをdown後、１つ以上のノードをremoveし、リーダーノードをjoin

  //リーダーノードをleave（remove）後、１つ以上のノードをdown
  //リーダーノードをleave（remove）後、downしたノードをjoin
  //リーダーノードをleave（remove）後、１つ以上のノードをleave
  //リーダーノードをleave（remove）後、１つ以上のノードをremove
  //リーダーノードをleave（remove）後、１つ以上のノードをleaveし、downする
  //リーダーノードをleave（remove）後、１つ以上のノードをleaveし、removeする
  //リーダーノードをleave（remove）後、１つ以上のノードをプロセスごと停止
  //リーダーノードをleave（remove）後、１つ以上のノードをleaveし、プロセスごと停止

  //リーダーノードをプロセスごと停止後、１つ以上のノードをdown
  //リーダーノードをプロセスごと停止後、downしたノードをjoin
  //リーダーノードをプロセスごと停止後、１つ以上のノードをleave
  //リーダーノードをプロセスごと停止後、１つ以上のノードをremove
  //リーダーノードをプロセスごと停止後、１つ以上のノードをleaveし、downする
  //リーダーノードをプロセスごと停止後、１つ以上のノードをleaveし、removeする
  //リーダーノードをプロセスごと停止後、１つ以上のノードをプロセスごと停止
  //リーダーノードをプロセスごと停止後、１つ以上のノードをleaveし、プロセスごと停止

}