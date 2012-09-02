package jp.ne.internavi.sync.hcp.dist

import akka.actor._
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.xml.XML
import org.specs2.mutable._
import org.specs2.specification.Scope
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig, BriseisThriftServer, THsHaServer}


object TestConfig1 extends BriseisConfig {
  //edit
  var app_no = "app_1"
  val hostname = "127.0.0.1"
  var port = 7919  
  val lookup = ("127.0.0.1", 2551)
  val rose_server = ("127.0.0.1", 7819)
  val autoJoin = true
  //do not edit  
  val nodeInfo = (app_no, hostname, port)
    
  val server = new BriseisThriftServer with THsHaServer {   
  }  
}



class CalcIdSpec extends Specification {  
  
  "SHA1Hasher.half" should {
    "return hash value length 20" in new context {
      val halfhasher = SHA1Hasher.half("hoghoge")
      halfhasher must have length 20
    }
  }

  "router.calcHostId" should {
    "return hash value length 40" in new context {
      val testTable = RoutingTable(System.currentTimeMillis(), List.empty, Set.empty)
      router.updateRoutingTable(testTable)      
      router.calcHostId(address) must have length 40
    }

    "return hash value when 0 node exists" in new context {
      val testTable = RoutingTable(System.currentTimeMillis(), List.empty, Set.empty)
      router.updateRoutingTable(testTable)
      router.calcHostId(address) must have length 40
    }

    "return hash value when 1 node exists" in new context {
      //first node
      val uri = "akka://ChordSystem-2552@127.0.0.1:2552"
      val id = router.calcHostId(uri)
      val first = Route(id,uri, Status.Up,"127.0.0.1",7819,1)
      //update 
      val testTable = RoutingTable(System.currentTimeMillis(), List(first), Set.empty)
      router.updateRoutingTable(testTable)
      //test 
      router.calcHostId(address) must have length 40
    }

    "return hash value when 2 node exists" in new context {
      
      val first = Route(router.calcHostId("akka://ChordSystem-2552@127.0.0.1:2552"),
			"akka://ChordSystem-2552@127.0.0.1:2552", Status.Up,"127.0.0.1",7819,1)      
      router.updateRoutingTable(RoutingTable(System.currentTimeMillis(), List(first), Set.empty))

      
      val second = Route(router.calcHostId("akka://ChordSystem-2553@127.0.0.1:2553"),
			 "akka://ChordSystem-2553@127.0.0.1:2553", Status.Up,"127.0.0.1",7819,1)     
      router.updateRoutingTable(RoutingTable(System.currentTimeMillis(), router.currentTable.table :+ second, Set.empty))
      
      //test       
      router.calcHostId(address) must have length 40
    }

    //TODO add over 15 node table test    
   
  }  
  
  trait context extends Scope {
    import akka.util.FiniteDuration;
    import java.util.concurrent.TimeUnit;
        
    val system = ActorSystem("test")
    val failureDetector =  
      new AccrualFailureDetector(system, 8.0, 10000, 
				 akka.util.Duration(100, "millis"), 
				 akka.util.Duration(3000,"millis"),  
				 akka.util.Duration(1000,"millis"), AccrualFailureDetector.realClock)
    val router = Router(system, TestConfig1, failureDetector)        
    val address = "hgoe"
  }

}
