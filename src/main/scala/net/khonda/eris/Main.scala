package net.khonda.eris

import net.khonda.eris.config.{Eris => ErisConfig}
import com.twitter.util.Eval
import java.io.File

object Main extends {

  var config: ErisConfig = _
  
  def main(args: Array[String]) {

    config  = Eval[ErisConfig](new File(args(0)))
    println("Eris Running as "+config.mode+" mode")

  }

}
