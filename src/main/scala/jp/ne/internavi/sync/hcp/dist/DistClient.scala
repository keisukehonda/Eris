package jp.ne.internavi.sync.hcp.dist

import com.twitter.conversions.time._

import org.apache.thrift.protocol.{TProtocol, TBinaryProtocol}
import org.apache.thrift.transport.{TSocket, TFramedTransport}

class DistClient(hostname: String, port: Int) {
 
  def socketTimeout = 50.seconds
	
  val socket = new TSocket(hostname, port, socketTimeout.inMilliseconds.toInt)
  val transport = new TFramedTransport(socket)
  val protocol = new TBinaryProtocol(transport)
  val client = new thrift.Rowz.Client.Factory().getClient(protocol)

  transport.open
  
  //p2p network command
  def join(hostname: String, port: Int) = client.join(hostname, port)
  def remove(hostname: String, port: Int) = client.remove(hostname, port)
  def down(hostname: String, port: Int) = client.down(hostname, port)
  def leave(hostname: String, port: Int) = client.leave(hostname, port)
  def leaveAction(hostname: String, port: Int): Int = client.leaveAction(hostname, port)
  def activateLeavePicker(uri: String): Int = client.activateLeavePicker(uri)
  def delmark() = client.delmark()
  def shutdown() = client.shutdown()

  def stats() = client.stats()
  def genID(key: String) = client.genID(key)
  def get(id: String) = client.get(id)
  def getHosts(id: String) = client.getHosts(id)
  
  //data manipulation
  def get_hint(offset: Int, limit: Int, query: String, level: thrift.ConsistencyLevel)
  = client.get_hint(offset, limit, query, level)
  
  def put_hint(key: String, hint: thrift.hint, level: thrift.ConsistencyLevel)
  = client.put_hint(key, hint, level)

  def del_hint(key: String, level: thrift.ConsistencyLevel)
  = client.del_hint(key, level)

  def get_ids(table: String, offset: Int, limit: Int, query: String, level: thrift.ConsistencyLevel) 
  = client.get_ids(table, offset, limit, query, level)

  def put_delivery_wall_messages(key: String, wall: thrift.delivery_wall_messages, level: thrift.ConsistencyLevel)
  = client.put_delivery_wall_messages(key, wall, level)
  	
  def get_delivery_wall_messages(key: String, query: String, level: thrift.ConsistencyLevel)
  = client.get_delivery_wall_messages(key, query, level)

  def delete_delivery_wall_messages(key: String, query: String, level: thrift.ConsistencyLevel) 
  = client.delete_delivery_wall_messages(key, query, level)  

  def get_column_count(key: String, tablename: String, colname: String, query: String, level: thrift.ConsistencyLevel)
  = client.get_column_count(key, tablename, colname, query, level)

  def get_column_long(key: String, tablename: String, colname: String, query: String, level: thrift.ConsistencyLevel)
  = client.get_column_long(key, tablename, colname, query, level)

  def set_column_bool(key: String, tablename: String, colname: String, column: thrift.column_bool, query: String, level: thrift.ConsistencyLevel) 
  =client.set_column_bool(key, tablename, colname, column, query, level)

  def put_delivery_images(key: String, image: thrift.delivery_images, level: thrift.ConsistencyLevel)
  = client.put_delivery_images(key, image, level)

  def get_delivery_images(key: String, query: String, level: thrift.ConsistencyLevel )
  = client.get_delivery_images(key, query, level)

  def delete_delivery_images(key: String, query: String, level: thrift.ConsistencyLevel)
  = client.delete_delivery_images(key, query, level)

  def put(table: String, key: String, data: Any, level: thrift.ConsistencyLevel): Long = {    
    table match {
      case "delivery_wall_messages" => put_delivery_wall_messages(key, data.asInstanceOf[thrift.delivery_wall_messages], level)
      case "delivery_sources" => put_delivery_images(key, data.asInstanceOf[thrift.delivery_images], level)
      case "handoff" => put_hint(key, data.asInstanceOf[thrift.hint], level)
      case _ => 0L
    }
  }

  def delete(table: String, key: String, query: String, level: thrift.ConsistencyLevel): Long = {
    table match {
      case "delivery_wall_messages" => delete_delivery_wall_messages(key, query, level)
      case "delivery_sources" => delete_delivery_images(key, query, level)
      case _ => 0L
    }
  }

}
