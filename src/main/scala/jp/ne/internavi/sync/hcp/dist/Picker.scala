package jp.ne.internavi.sync.hcp.dist

import akka.actor._
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.concurrent.ops._
import jp.ne.internavi.sync.hcp.dist.thrift.ConsistencyLevel
import jp.ne.internavi.sync.hcp.dist.thrift.conversions.Hint._

trait Picker {
  import thrift.conversions.Hint._  

  val level: thrift.ConsistencyLevel = ConsistencyLevel.NO_REPLICA

  def pick_put(id: Long, table: String, fromConn: DistClient, toConn: DistClient) = {
    //get data from table where id=key.value
    val key = id.toString
    val query = "id="+id.toString    
    val getdata = try { 
      table match {
	case "delivery_wall_messages" => fromConn.get_delivery_wall_messages(key, query, level)
	case "delivery_sources" => fromConn.get_delivery_images(key, query, level)
	case _ => List.empty.asJava
      }
    } catch { case e: Exception => println(e); List.empty.asJava }

    //put data to table    
    getdata.asScala foreach( data => {
      val res = try { toConn.put(table, key, data, level) } catch { case e:Exception => println(e); 0L }      
      res match {
	case 1 => {
	  //delete hint and data from node
	  table match {
	    case "delivery_wall_messages" => fromConn.delete_delivery_wall_messages(key, query, level)
	    case "delivery_sources" => fromConn.delete_delivery_images(key, query, level)
	    case _ => 0L
	  }
	  fromConn.del_hint(key, level)
	  println("res="+res)
	}
	case _ => //TODO
      }	
    })
  }

  def pick_set(hint: Hint, fromConn: DistClient, toConn: DistClient) = {
    val key = hint.id.toString

    //set method excecute
    val res = try {
      val column = new thrift.column_bool(hint.bool_value)
      toConn.set_column_bool(key, hint.table_name, hint.col_name, column, hint.query, level)
    }
    res match {
      case 1 => fromConn.del_hint(key, level); println("res="+res)
      case _ => //TODO  
    }
  }

  def pick_delHint(hint: Hint, fromConn: DistClient, toConn: DistClient) = {
    val key = hint.id.toString
    
    val res = try {
      hint.table_name match {
	case "delivery_wall_messages" => toConn.delete_delivery_wall_messages(key, hint.query, level)
	case "delivery_sources" => toConn.delete_delivery_images(key, hint.query, level)
	case _ => 0
      }
    }
    res match {
      case 1 => fromConn.del_hint(key, level); println("res="+res)
      case _ => //TODO  
    }
  }

  def getConn(host:String, dbport: Int): DistClient = new DistClient(host, dbport)
}

/*
class DelMarker extends Picker {
  
  import scala.concurrent.ops._
  import jp.ne.internavi.sync.hcp.dist.thrift.column_bool

  case class State(version: Long, future: () => Long)
  val state = {        
    new AtomicReference[State](State(System.currentTimeMillis(), () => 1L))
  }
  
  def update(self: Route, rt: RoutingTable, table: String): Unit = {
    if(isDone) {     
      //publish SQL
      val query = "id>0"      
      val res = future {
	getConn(self.dbhost, self.dbport).set_column_bool("", table, "validity", new column_bool(true), query, level)
      }      
      changeState(State(System.currentTimeMillis(), res))
    }
  }
  
  @tailrec
  private def changeState(newState: State){
    val current = state.get
    if(!state.compareAndSet(current, newState)) changeState(newState) //recur if we fail the update
    else {
      println("del marker state update isDone:"+state.get.version)
    }
  }

  //def isDone: Boolean = true
  def isDone: Boolean = {
    val res = state.get.future.apply
    println("last del marker res:"+res)
    res  match {
      case 1L => true
      case 0L => false	
    }
  }    
}

object JoiningPicker {
  def apply(table: String,
	    predessorOfTo: Route,
	    to: Route, 
	    from: Route) = {
    new JoiningPicker(table, predessorOfTo, to, from)
  }
}
*/

class DelMarker extends Picker {
  
  import jp.ne.internavi.sync.hcp.dist.thrift.column_bool
  val col_bool = new column_bool(true)
    
  case class State(isDone: Boolean)
  val state = {        
    new AtomicReference[State](State(true))
  }

  def update(target: Route, router: Router, table: String) = {
    if(isDone) {
      changeState(State(false)) 
      val prime = target
      val succ = router.getSuccessor(prime)
      val pre = router.getPredecessor(prime)
      val pre_pre = router.getPredecessor(pre)
      val primeConn = getConn(prime.dbhost, prime.dbport)
      spawn { task(0, 5, table, primeConn, prime, succ, pre, pre_pre) }      
    }
  }

  //@tailrec
  private def task(offset: Int, limit: Int, table: String, primeConn: DistClient,
		   prime: Route, succ: Route, pre: Route, pre_pre: Route): Unit = {
    val query = "validity = 0"
    val ids = primeConn.get_ids(table, offset, limit, query, level).asScala.toList
    ids foreach {
      key => {
	val id = SHA1Hasher.genID(key.toString)
	val inPrime = SHA1Hasher.isInInterval(id, pre.id, prime.id)
	val inSucc = SHA1Hasher.isInInterval(id, prime.id, succ.id)
	val inPre = SHA1Hasher.isInInterval(id, pre_pre.id, pre.id)	
	if(!(inPrime || inSucc || inPre)) { // out range	  
	  primeConn.set_column_bool("", table, "validity", col_bool, "id="+id, level)
	}
      }
    }
    if (ids.nonEmpty) task(offset+limit, limit, table, primeConn, prime, succ, pre, pre_pre) else {
      println("del marker finish")
      changeState(State(true))
    }
  }
    
  @tailrec
  private def changeState(newState: State){
    val current = state.get
    if(!state.compareAndSet(current, newState)) changeState(newState) //recur if we fail the update
    else {
      println("del marker state update isDone:"+state.get.isDone)
    }
  }
  
  def isDone: Boolean = state.get.isDone
  
}

object JoiningPicker {
  def apply(table: String,
	    predessorOfTo: Route,
	    to: Route, 
	    from: Route) = {
    new JoiningPicker(table, predessorOfTo, to, from)
  }
}

//picker for Joining and Leaving
class JoiningPicker private (table: String,
			     predessorOfTo: Route,
			     to: Route, 
			     from: Route) extends Picker {
  

  case class State(isDone: Boolean)
  val state = {        
    new AtomicReference[State](State(false))
  }

  val isSingle: Boolean = to.id == from.id //nothing todo
  lazy val fromConn: DistClient = this.getConn(from.dbhost, from.dbport)
  lazy val toConn: DistClient = this.getConn(to.dbhost, to.dbport)
  

  println("picker task start")
  println("from: "+from)
  println("to: "+to)
  println("predessorOfTo: "+predessorOfTo)

  def start = {
    //start task async  
    if(!isSingle) {
      spawn { task(0, 5) } 
    }else { 
      println("Single cluster no pick task")
      changeState(State(true)) 
    }
  }
  
  @tailrec
  private def task(offset: Int, limit: Int): Unit = {
    val query = "validity = 0"
    val ids = fromConn.get_ids(table, offset, limit, query, level).asScala.toList
    ids foreach {
      key => {
	//check this data is "to" node interval	
	val id = SHA1Hasher.genID(key.toString)
	if(SHA1Hasher.isInInterval(id, predessorOfTo.id, to.id)) {	  
	  println("pick "+key.value+":"+id)
	  pick_put(key.value, table, fromConn, toConn)
	} else {
	  println("not pick "+key.value+":"+id)
	}	
      }
    }
    if (ids.nonEmpty) task(offset+limit, limit) else {      
      println("task finish")
      changeState(State(true))
    }
  }

  @tailrec
  private def changeState(newState: State){
    val current = state.get
    if(!state.compareAndSet(current, newState)) changeState(newState) //recur if we fail the update
    else {
      println("picker state update isDone:"+state.get)
    }
  }
  
  def isDone: Boolean = isSingle || state.get.isDone

}


object HandoffPicker {
  def apply(rt: RoutingTable, self: Route) = {
    new HandoffPicker(rt, self)
  }
}

class HandoffPicker private (rt: RoutingTable, self: Route) {

  import thrift.conversions.Hint._  

  case class WorkerState(version: Long, workers: List[Worker])
  val state = {
    val latest = WorkerState(0L, List.empty)
    new AtomicReference[WorkerState](latest)
  }

  //start workers
  start(rt) 

  
  @tailrec
  final def updateWorkers(rt: RoutingTable): Unit = { 
    val localState = state.get
    val winningWorkers = if(localState.version == rt.version) localState.workers
    else if(localState.version > rt.version) localState.workers
    else rt.table map { route => new Worker(route) }
    
    val newState = localState copy (workers = winningWorkers)    
    if(!state.compareAndSet(localState, newState)) updateWorkers(rt) //recur if we fail the update
  }

  private def start(rt: RoutingTable): Unit = {
    updateWorkers(rt)
    //all worker start again
    state.get.workers foreach (w => {
      w.changeState(false)
      w.start
    })
  }
	  

  def restart(rt: RoutingTable): Unit = {
    if(isDone) {
      //update workers 
      updateWorkers(rt)
      //all worker start again
      state.get.workers foreach (w => {
	w.changeState(false)
	w.start
      })
    }
  }

  def isDone: Boolean = state.get.workers.filter(w => w.state.get.isDone == false).isEmpty

  class Worker(route: Route) extends Picker {
    lazy val fromConn: DistClient = this.getConn(route.dbhost, route.dbport)
    lazy val toConn: DistClient = this.getConn(self.dbhost, self.dbport)    
    
    case class State(isDone: Boolean)    
    val state = {        
      new AtomicReference[State](State(false))
    }    
    
    def start = {      
      //start task async  
      if(route.uri != self.uri && route.state == Status.Up) {
	println("handoff picker start for:"+route.uri)
	spawn { 
	  task("put", 0, 5)
	  task("set", 0, 5)
	  task("delete", 0, 5)
	} 
      } else {	
  	println("picker nothing todo for:"+route.uri+":"+route.state)
	changeState(true) 
      }
    }
    
    @tailrec
    private def task(method: String, offset: Int, limit: Int): Unit = {      
      val query = "node_to='"+self.uri+"' AND method_name='"+method+"' AND validity=0"
      val hints = fromConn.get_hint(offset, limit, query, level).asScala.toList

      hints foreach (hint => {
	method match {
	  case "put" => pick_put(hint.id, hint.table_name, fromConn, toConn)
	  case "set" => pick_set(hint.fromThrift, fromConn, toConn)
	  case "delete" => pick_delHint(hint.fromThrift, fromConn, toConn)
	  case _ =>
	}
      })
      if(hints.nonEmpty) {
	task(method, offset+limit, limit)
      } else {
	println(method+" task finish")
	if(method=="delete")changeState(true)
      }
    }    

    @tailrec
    final def changeState(isDone: Boolean){
      val newState = State(isDone)
      val current = state.get
      if(!state.compareAndSet(current, newState)) changeState(newState.isDone) //recur if we fail the update
	else {
	  println("hopicker isDone:"+state.get+" for:"+route.uri)
	}
    }
    
  }  

}
