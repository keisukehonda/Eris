package jp.ne.internavi.sync.hcp.dist

import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig}
import com.twitter.util.Eval
import java.io.File

object Main extends {

  var service: Briseis = _
  var config: BriseisConfig = _
  
  def main(args: Array[String]) {
        
    config  = Eval[BriseisConfig](new File(args(0)))
    
    //akka system start
    val node = new Node(config)
                         
    //Thrift start
    val service = new Briseis(config, node)
    service.start()
    
    println("Chord Running.")
  }

}
