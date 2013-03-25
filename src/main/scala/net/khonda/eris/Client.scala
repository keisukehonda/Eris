package net.khonda.eris

import akka.actor._
import akka.remote.RemoteScope
import com.typesafe.config.ConfigFactory
import ch.qos.logback._
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Client {

  def apply(client_no: String, server: String, server_port: Int) = {    
    new Client(client_no, server, server_port)
  }
 
}

class Client private(val client_no: String, val server: String, val server_port: Int) extends Peer {
  import net.khonda.eris.column._
  import ConsistencyLevel._
    
  val logger = LoggerFactory.getLogger(classOf[Client])
  //create akka system  
  val system = ActorSystem("ChordSystem-"+client_no, ConfigFactory.load().getConfig(client_no).withFallback(akkaConfig))

  lazy val self: Address = getAddressFromConfig(client_no)
    
  //Sender and Reciever
  val sender = system.actorOf(Props(new Sender).withDeploy(Deploy(scope=RemoteScope(self))), name = "cSender")
  println("Client[Sender] start")
  val reciever = system.actorOf(Props(new Receiver).withDeploy(Deploy(scope=RemoteScope(self))), name = "cReceiver")
  println("Client[Reciever] start")

  def serverAddress: Address = AddressFromURIString(getUri(server, server_port))
  
  class Sender extends Actor {

    def receive = {
      case Put(keyspace: String,
	       key: Long,
	       columnPath: ColumnPath,
	       value: Column,
	       timestamp: Long,	  
	       level: ConsistencyLevel) => {
		
      }
      case _ => {
      }
    }

  }

  class Receiver extends Actor {

    def receive = {
      case _ => {
      }
    }

  }

  //public API for User
  def put(keyspace: String, 
	  key: Long,
	  columnPath: ColumnPath,
	  value: Column,
	  timestamp: Long,	  
	  level: ConsistencyLevel): Unit = {
    sender ! Put(keyspace, key, columnPath, value, timestamp, level)
  }

}
