package jp.ne.internavi.sync.hcp.dist

import org.specs2.mutable._
import scala.actors.Futures._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig}
import com.twitter.util.Eval
import java.io.File
import akka.actor._
import Status._

class NodeSpec extends Specification {

  args(sequential=true)

  var config: BriseisConfig = Eval[BriseisConfig](new File("config/development_app1.scala"))
  val node = new Node(config)
  var service: Briseis = new Briseis(config, node)

  "A Briseis " should {

    "Node start success" in {
      service.start() must not (throwA[Exception])
      //node.router.state.toString must contain("Joining")
      alarm(45*1000)
      node.router.state.toString must contain("Up")
    }

    "Node status change \"Down\" => \"Leaving\" => \"Removed\"" in {
      val uri = config.getUri("127.0.0.1", 2552)
      val address = (AddressFromURIString(uri))
      // Down
      node.down(address)
      alarm(15*1000)
      node.router.state.toString must contain("Down")
      // Leaving
      node.leave(address)
      alarm(15*1000)
      node.router.state.toString must contain("Leaving")
      // Remove
      node.remove(address)
      alarm(15*1000)
      node.router.state.toString must contain("Removed")
    }

    "Routing table is update" in {
      val newTable:RoutingTable = RoutingTable(5555555555555L, List.empty, Set.empty)
      node.router.updateRoutingTable(newTable)
      val update:String = "State("+newTable+")"
      node.router.state.toString must be_==(update)
      
    }

    "Successor is me" in {
      node.router.getSuccessor.toString must be_==(node.router.getNode.toString)
    }

    "Predecessor is me" in {
      node.router.getPredecessor.toString must be_==(node.router.getNode.toString)
    }

    "Node shutdown success" in {
      node.shutdown() must not (throwA[Exception])
    }

  }
}
