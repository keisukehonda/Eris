package net.khonda.eris.db

import akka.actor._
import akka.remote.RemoteScope
import ch.qos.logback._
import com.typesafe.config.ConfigFactory
import net.khonda.eris._
import net.khonda.eris.config.{Eris => ErisConfig}
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Db(config: ErisConfig) extends Peer {

  //akka system start
  val logger = LoggerFactory.getLogger(classOf[Db])  
  val port = ConfigFactory.load().getConfig(config.db_no).getInt("akka.remote.netty.port")
  val system = ActorSystem("ChordSystem-"+port,
			   ConfigFactory.load().getConfig(config.db_no).withFallback(akkaConfig))

  val self: Address = AddressFromURIString(config.getUri(config.hostname, port))

  val stabilizer = new Stabilizer(system, self)

}
