package net.khonda.eris

import net.khonda.eris.config.{Eris => ErisConfig}
import net.khonda.eris.node._
import net.khonda.eris.db._
import com.twitter.util.Eval
import java.io.File

object Main extends {

  var config: ErisConfig = _
  
  def main(args: Array[String]) {

    config  = Eval[ErisConfig](new File(args(0)))
    
    //akka system start
    val node = if (config.db_mode) new Db(config) else new Node(config)
    
    println("Eris Running as "+ (if (config.db_mode) "db" else "node") +" mode")

  }

}
