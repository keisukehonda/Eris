package net.khonda.eris

import net.khonda.eris.config.{Eris => ErisConfig}
import net.khonda.eris.node._
import com.twitter.util.Eval
import java.io.File

object Main extends {

  var config: ErisConfig = _
  
  def main(args: Array[String]) {

    //config  = Eval[ErisConfig](new File(args(0)))    
    config = args(0) match {
      case "config/app1.scala" => net.khonda.eris.config.app1
      case "config/app2.scala" => net.khonda.eris.config.app2
      case _                   => net.khonda.eris.config.app1
    }
    

    //akka system start
    val node = new Node(config)
    
    println("Eris Running as "+config.mode+" mode")

  }

}
