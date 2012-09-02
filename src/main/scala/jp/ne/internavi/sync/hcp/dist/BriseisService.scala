package jp.ne.internavi.sync.hcp.dist
        
import akka.actor._
import ch.qos.logback._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import jp.ne.internavi.sync.hcp.dist.config.{Briseis => BriseisConfig}
import jp.ne.internavi.sync.hcp.dist._
import jp.ne.internavi.sync.hcp.dist.thrift.conversions.Wall._


class BriseisService(config: BriseisConfig, node: Node) extends thrift.Rowz.Iface {
  import Status._
  				
  def logger = LoggerFactory.getLogger(classOf[BriseisService])

  //p2p network command
  //
  //

  def join(hostname: String, port: Int): String = {    
    val uri = config.getUri(hostname, port)    
    node.join(AddressFromURIString(uri))
    "join command received "+uri
  }

  def remove(hostname: String, port: Int): String = {
    val uri = config.getUri(hostname, port)   
    node.remove(AddressFromURIString(uri))
    "remove command received "+uri
  }

  def down(hostname: String, port: Int): String = {
    val uri = config.getUri(hostname, port)   
    node.down(AddressFromURIString(uri))
    "down command received "+uri
  }

  def leave(hostname: String, port: Int): String = {
    //TODO not accept if leaving node stil exists 
    
    //find leader node and dipatch this command
    val leader = node.router.getLeader
    val host = AddressFromURIString(leader.uri).host
    host match {
      case Some(lhost) 
      if node.broker.getConn(lhost, leader.port).leaveAction(hostname, port) ==1 => {
	"leave command received "+hostname
      }
      case _ => "fail leave command "+hostname
    }    
  }

  //leaders action of leave
  def leaveAction(hostname: String, port: Int): Int = {    
    val uri= config.getUri(hostname, port)
    //target node status -> Leaving
    node.leave(AddressFromURIString(uri))    

    //picker activate in leaving node    
    node.router.getRouteByURI(uri) match {
      case Some(route) => {	
	val successor = node.router.getSuccessor(route)
	if(successor.uri != route.uri) {
	  if(route.uri == node.router.getNode.uri) activateLeavePicker(uri) else {
	    AddressFromURIString(route.uri).host match {
	      case Some(host) => node.broker.getConn(host, route.port).activateLeavePicker(uri)	      
	      case _ => 0	      
	    }
	  }
	} else println("nothing todo "); 1	
      }
      case _ => 0
    }
  }

  def activateLeavePicker(uri: String): Int = node.activateLeavePicker(uri)

  def delmark(): String = {
    node.delmarkAction()
    "delmark command recived "+node.router.self
  }

  def shutdown(): String = {  
    node.shutdown()
    "shutdown command recived "+node.router.self
  }
  
  
  def stats() = { 
    val current = java.util.Calendar.getInstance()
    val diff  = current.getTimeInMillis - node.startTime.getTimeInMillis
    val localTable = node.router.currentTable.table
    new thrift.statistics(node.startTime.toString,
			  diff.toString,
			  1.0,
			  localTable.toString)
  }
  
  def genID(key: String) = {
    SHA1Hasher(key)
  }
  
  def get(id: String) = {  	
    node.broker.lookup(id).toString
  }
  
  def getHosts(id: String): java.util.List[String] = {
    logger.debug("getHosts "+id)
    if(id.length != 40) throw new thrift.RowzException("Invalid id value")    
    List.empty[String].asJava  	
  }

  //data manipulation api
  //
  //

  //wall
  def put_delivery_wall_messages(key: String, wall: thrift.delivery_wall_messages, level: thrift.ConsistencyLevel): Long = {    
    put("delivery_wall_messages", key, wall.id, wall, level)
  }
  
  def get_delivery_wall_messages(key: String, query: String, level: thrift.ConsistencyLevel) = {
    
    @tailrec
    def get(hosts: Seq[Route]): java.util.List[thrift.delivery_wall_messages] = {      
      if (hosts.isEmpty) List.empty.asJava
      else {
	val res =  try { 
	  node.broker.getRoseConn(hosts.head).get_delivery_wall_messages(key, query, level)
	} catch { case e: Exception => val non: java.util.List[thrift.delivery_wall_messages] =  List.empty.asJava; non }
	if(res.size != 0 ) res else get(hosts.tail)
      }
    }        
    get(node.broker.getHosts(key))
    
  }  
  
  def delete_delivery_wall_messages(key: String, query: String, level: thrift.ConsistencyLevel) = {
    delete("delivery_wall_messages", key, query, level)
  }

  //source
  def put_delivery_images(key: String, image: thrift.delivery_images, level: thrift.ConsistencyLevel) = {
    put("delivery_sources", key, image.source_id, image, level)   
  }
  
  def get_delivery_images(key: String, query: String, level: thrift.ConsistencyLevel ) = {

    @tailrec
    def get(hosts: Seq[Route]): java.util.List[thrift.delivery_images] = {      
      if (hosts.isEmpty) List.empty.asJava
      else {
	val res = try {
	  node.broker.getRoseConn(hosts.head).get_delivery_images(key, query, level)
	} catch { case e: Exception => val non: java.util.List[thrift.delivery_images] =  List.empty.asJava; non }
	if(res.size != 0 ) res else get(hosts.tail)
      }
    }        
    get(node.broker.getHosts(key))

  }
 
  def delete_delivery_images(key: String, query: String, level: thrift.ConsistencyLevel) = {
    delete("delivery_sources", key, query, level)
  }

  //column base query
  def get_column_count(key: String, tablename: String, colname: String, query: String, level: thrift.ConsistencyLevel) = {
    
    @tailrec
    def get(hosts: Seq[Route]): Long = {      
      if (hosts.isEmpty) 0L
      else {
	val res = try {
	  node.broker.getRoseConn(hosts.head).get_column_count(key, tablename, colname, query, level)
	} catch { case e: Exception => 0L }
	if(res != 0L ) res else get(hosts.tail)
      }
    }        
    get(node.broker.getHosts(key))
    
  }
    
  def get_column_long(key: String, tablename: String, colname: String, query: String, level: thrift.ConsistencyLevel) = {

    @tailrec
    def get(hosts: Seq[Route]): java.util.List[thrift.column_long] = {      
      if (hosts.isEmpty) List.empty.asJava
      else {
	val res = try {
	  node.broker.getRoseConn(hosts.head).get_column_long(key, tablename, colname, query, level)
	} catch { case e: Exception => val non: java.util.List[thrift.column_long] =  List.empty.asJava; non }
	if(res.size != 0 ) res else get(hosts.tail)
      }
    }        
    get(node.broker.getHosts(key))

  }   
  
  def set_column_bool(key: String, tablename: String, colname: String, column: thrift.column_bool, query: String, level: thrift.ConsistencyLevel) = {
    set(key, tablename, colname, column, query, level)
  }

  //internal api
  //
  //
  private def findHost(key: String): Option[Route] = {    
    //find Up node from primary successor, and predessor 
    node.broker.getHosts(key).find(route => route.state == Status.Up)
  }  

  private def put(table: String, key: String, id: Long, data: Any, level: thrift.ConsistencyLevel): Long = {

    val broker = node.broker 
    lazy val localRose = broker.getRoseConn(node.router.getNode)
    
    val hosts = broker.getHosts(key)        
    val res = if(level != thrift.ConsistencyLevel.NO_REPLICA) {
      hosts map { host => 
	if(host.state != Status.Up) {	
	  //handoff
	  val hint = new thrift.hint(id, host.uri, table, "put", "", "", false , false, "")
	  try {
	    localRose.put("handoff", key, hint, thrift.ConsistencyLevel.NO_REPLICA)	    
	    localRose.put(table, key, data, thrift.ConsistencyLevel.NO_REPLICA)
	  } catch { case e: Exception => 0 }
	} else {
	  try broker.getRoseConn(host).put(table, key, data, level) catch { case e: Exception => 0 }//TODO handoff
	}
      }
    } else List(try localRose.put(table, key, data, thrift.ConsistencyLevel.NO_REPLICA) catch { case e: Exception => 0 })

    if(res.sum > 0) 1 else 0

  }  

  private def delete(table: String, key: String, query: String, level: thrift.ConsistencyLevel) = {

    val broker = node.broker
    lazy val localRose = broker.getRoseConn(node.router.getNode)
    
    val hosts = broker.getHosts(key)    
    val res = if(level != thrift.ConsistencyLevel.NO_REPLICA) {
      hosts map { host => 
	if(host.state != Status.Up) {
	  //handoff	
	  val hint = new thrift.hint(key.toLong, host.uri, table, "delete", query, "", false, false, "")
	  try {
	    localRose.put("handoff", key, hint, thrift.ConsistencyLevel.NO_REPLICA)
	  } catch { case e: Exception => 0 }	  
	} else {
	  try broker.getRoseConn(host).delete(table, key, query, thrift.ConsistencyLevel.NO_REPLICA) catch { case e: Exception => 0 }	  
	}
      }
    } else List( try localRose.delete(table, key, query, thrift.ConsistencyLevel.NO_REPLICA) catch { case e: Exception => 0 })
 
    if(res.sum > 0) 1 else 0

  }

  private def set(key: String, table: String, colname: String, column: thrift.column_bool, query: String, level: thrift.ConsistencyLevel) = {     
    val broker = node.broker
    lazy val localRose = broker.getRoseConn(node.router.getNode)
    
    val hosts = broker.getHosts(key)
    val res = if(level != thrift.ConsistencyLevel.NO_REPLICA) {
      hosts map { host => 
	if(host.state != Status.Up) {
	  //handoff
	  val bool = column.value
	  val hint = new thrift.hint(key.toLong, host.uri, table, "set", query, colname, bool, false, "")
	  try {
	    localRose.put("handoff", key, hint, thrift.ConsistencyLevel.NO_REPLICA)	
	  } catch { case e: Exception => 0 }
	} else {
	  try { 
	    broker.getRoseConn(host).set_column_bool(key, table, colname, column, query, thrift.ConsistencyLevel.NO_REPLICA) 
	  } catch { case e: Exception => 0 }
	}
      }
    } else List(try localRose.set_column_bool(key, table, colname, column, query, thrift.ConsistencyLevel.NO_REPLICA) catch {
      case e: Exception => 0 })
    
    if(res.sum > 0) 1 else 0

  }
  
  //dummy implementation only use internal call with Rose directly
  def get_ids(table: String, offset: Int, limit: Int, query: String, level: thrift.ConsistencyLevel) = {
    List.empty.asJava
  }

  def put_hint(key: String, hint: thrift.hint, level: thrift.ConsistencyLevel) = {
    1L
  }
  
  def get_hint(offset: Int, limit: Int, query: String, level: thrift.ConsistencyLevel) = {
    List.empty.asJava
  }
  
  def del_hint(key: String, level: thrift.ConsistencyLevel) = {   
    1L
  }
}
