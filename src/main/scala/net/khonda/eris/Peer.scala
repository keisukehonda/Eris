package net.khonda.eris

import com.typesafe.config.ConfigFactory

trait Peer {

  val startTime =  java.util.Calendar.getInstance(new java.util.Locale("ja", "JP", "JP")) //TODO
  val akkaConfig = ConfigFactory.load()

}
