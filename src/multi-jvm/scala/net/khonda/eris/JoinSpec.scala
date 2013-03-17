package net.khonda.eris
 
import org.scalatest.{ BeforeAndAfterAll, WordSpec }
import org.scalatest.matchers.MustMatchers
import akka.remote.testkit.MultiNodeConfig
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import akka.actor.{Props, Actor} 
 
object MultiNodeExampleConfig extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
}

class MultiNodeExampleSpecMultiJvmNode1 extends MultiNodeExample
class MultiNodeExampleSpecMultiJvmNode2 extends MultiNodeExample
 
class MultiNodeExample extends MultiNodeSpec(MultiNodeExampleConfig)
  with WordSpec with MustMatchers with BeforeAndAfterAll with ImplicitSender {
 
  import MultiNodeExampleConfig._
 
  def initialParticipants = roles.size
 
  "A MultiNodeExample" must {
 
    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }
 
    "send to and receive from a remote node" in {
      runOn(node1) {
	// wait for the other node to have deployed an actor
        enterBarrier("deployed")
        val ponger = system.actorFor(node(node2) / "user" / "ponger")
        ponger ! "ping"
        expectMsg("pong")
      }
 
      runOn(node2) {
	// deploy the ponger actor
        system.actorOf(Props(new Actor {
          def receive = {
            case "ping" => sender ! "pong"
          }
        }), "ponger")
        enterBarrier("deployed")
      } 
 
      // wait for all nodes to finish the test
      enterBarrier("finished")
    }
  }
 
  // hook the MultiNodeSpec into ScalaTest
  override def beforeAll() = multiNodeSpecBeforeAll() 
  override def afterAll() = multiNodeSpecAfterAll()
}
