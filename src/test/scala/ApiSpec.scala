package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import com.typesafe.config.ConfigFactory
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}
import java.util.concurrent.TimeUnit
import jp.ne.internavi.sync.hcp.dist._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ApiSpecConfig extends BriseisConfig {
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

class ApiSpec extends Specification {
  import ApiSpecConfig._
  args(sequential=true)
  val logger = LoggerFactory.getLogger(classOf[ApiSpec])

  var config = ApiSpecConfig
  config.app_no = "app_"+1
  //config.port = config.port + 1 //ConfigFactory.load().getConfig(config.app_no).getInt("akka.remote.netty.port")
  logger.debug("START NODE APP#1")
  val node = new Node(config)
  var service: Briseis = new Briseis(config, node)
  TimeUnit.SECONDS.sleep(10)

  "Briseis APIs" should {

    "Wall Put success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      var wall = new delivery_wall_messages(1L,101L,201L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      for (i <- 1000000000000000L to 1000000000000015L) {
        wall = new delivery_wall_messages(i,101L,201L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
        service.briseisService.put_delivery_wall_messages(i.toString, wall,ConsistencyLevel.PRIME) must not be_==(0)
        TimeUnit.SECONDS.sleep(1)
      }
      var res = service.briseisService.get_column_count("1000000000000000","delivery_wall_messages","ins", "id > 100000000000000",ConsistencyLevel.PRIME)
      res must be_==(16)
    }
    "Wall Get success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("id:1000000000000000")
      service.briseisService.get_delivery_wall_messages("1000000000000001","id > 1000000000000001",ConsistencyLevel.PRIME).toString must not contain("id:1000000000000000")
    }
    "Wall Update success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      //TODO 全カラムの更新を確認
      //global_id
      var wall = new delivery_wall_messages(1000000000000000L,1122L,201L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("1122")
      //product_own_id
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,3,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("2233")
      //category_id
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("332211")
      //service_id
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,5,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("4343")
      //view
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,6,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("555444")
      //provider_id
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,false,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("676767")
      //seen//true,false
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,false,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("seen:true")
      //validity//true,false
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1320238531,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("validity:true")
      //delivery_start
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("1431349642")
      //table_name
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name_UPDATE",10L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("table_name_UPDATE")
      //id1
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name_UPDATE",10101010L,11L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("10101010")
      //id2
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name_UPDATE",10101010L,1313131313L,12L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("1313131313")
      //id3
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name_UPDATE",10101010L,1313131313L,1234123412341234L,"tokyo","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("1234123412341234")
      //content_tokyo
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name_UPDATE",10101010L,1313131313L,1234123412341234L,"tokyo_UPDATE","irft","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("tokyo_UPDATE")
      //content_itrf
      wall = new delivery_wall_messages(1000000000000000L,1122L,2233L,332211,4343,555444,676767,true,true,1431349642,"table_name_UPDATE",10101010L,1313131313L,1234123412341234L,"tokyo_UPDATE","irft_UPDATE","") //must not (beNull)
      service.briseisService.put_delivery_wall_messages("1000000000000000", wall,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("irft_UPDATE")
    }
    "Wall Delete success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      service.briseisService.delete_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      service.briseisService.get_delivery_wall_messages("1000000000000000","id = 1000000000000000",ConsistencyLevel.PRIME).toString must contain("validity:true")
    }
    "Column count success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      service.briseisService.get_column_count("1000000000000001","delivery_wall_messages","ins", "id > 100000000000000",ConsistencyLevel.PRIME) must be_==(16)
    }
    "Column Get Long success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.column_bool
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      val res = service.briseisService.get_column_long("1000000000000001","delivery_wall_messages","ins", "id > 100000000000000",ConsistencyLevel.PRIME)
      res.toString must contain ("column_long(value:")
      res.size must be_==(16)
    }
    "Column set bool success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.column_bool
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_wall_messages
      //既読化
      var bool = new column_bool(true)
      service.briseisService.set_column_bool("1000000000000015","delivery_wall_messages","seen",bool, "id = 1000000000000015",ConsistencyLevel.PRIME)
      TimeUnit.SECONDS.sleep(1)
      //確認
      service.briseisService.get_delivery_wall_messages("1000000000000015","id = 1000000000000015",ConsistencyLevel.PRIME).toString must contain("seen:true")
      //未読化
      bool = new column_bool(false)
      service.briseisService.set_column_bool("1000000000000015","delivery_wall_messages","seen",bool, "id = 1000000000000015",ConsistencyLevel.PRIME)
      TimeUnit.SECONDS.sleep(1)
      //確認
      service.briseisService.get_delivery_wall_messages("1000000000000015","id = 1000000000000015",ConsistencyLevel.PRIME).toString must contain("seen:false")
      //複数既読化
      bool = new column_bool(true)
      service.briseisService.set_column_bool("1000000000000010","delivery_wall_messages","seen",bool, "id > 1000000000000009",ConsistencyLevel.PRIME)
      //確認
      service.briseisService.get_delivery_wall_messages("1000000000000010","id > 1000000000000009",ConsistencyLevel.PRIME).toString must not contain("seen:false")
    }
    "Source Put success" in {
      import java.nio.ByteBuffer
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_images
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      var buffer = ByteBuffer.wrap("HelloWorld".getBytes());
      val image = new delivery_images(1000000000000L,10000L,false,false,"type",buffer)
      service.briseisService.put_delivery_images("1000000000000",image,ConsistencyLevel.PRIME) must not be_==(0)
      TimeUnit.SECONDS.sleep(1)
      var res = service.briseisService.get_column_count("1000000000000","delivery_sources","ins", "source_id > 100000000000",ConsistencyLevel.PRIME)
      res must be_==(1)
    }
    "Source Get success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_images
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      service.briseisService.get_delivery_images("1000000000000", "source_id = 1000000000000",ConsistencyLevel.PRIME).toString must contain("source_id:1000000000000")
    }
    "Source Delete success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.delivery_images
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      service.briseisService.delete_delivery_images("1000000000000","source_id = 1000000000000",ConsistencyLevel.PRIME).toString must be_==("1")//must contain("validity:true")
    }
    "get ids success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      val res = service.briseisService.get_ids("delivery_wall_messages",0,5,"query",ConsistencyLevel.PRIME).toString
      res must contain("1000000000000000")
      res must contain("1000000000000001")
      res must contain("1000000000000002")
      res must contain("1000000000000003")
      res must contain("1000000000000004")
      res must not contain("1000000000000005")
    }
    "get stats success" in {
      import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
      val res = service.briseisService.stats().table
      res.toString must contain(node.router.self.toString)
    }
/*
//TODO
)hint
)put hint
import jp.ne.internavi.sync.hcp.dist.RowzClient
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
import jp.ne.internavi.sync.hcp.dist.thrift.hint
val c = new RowzClient("127.0.0.1",7819)
val hint = 

)get hint
import jp.ne.internavi.sync.hcp.dist.RowzClient
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
val c = new RowzClient("127.0.0.1",7819)
val hint = 
c.put_hint(
*/
  }// should

  node.shutdown()

}