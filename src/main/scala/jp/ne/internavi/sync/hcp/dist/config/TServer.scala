package jp.ne.internavi.sync.hcp.dist.config

import java.util.concurrent.ThreadPoolExecutor
import com.twitter.util.Duration
import com.twitter.conversions.time._
import org.apache.thrift
import jp.ne.internavi.sync.hcp.dist

class ThreadPool extends (String => ThreadPoolExecutor) {
  var stopTimeout = 60
  var minThreads = 1
  var maxThreads = 1
  var maxWaiters: Option[Int] = None

  def maxWaiters_=(n: Int) { maxWaiters = Some(n) }

  def apply(name: String): ThreadPoolExecutor = {
    if (maxThreads < minThreads) maxThreads = minThreads

    dist.thrift.TSelectorServer.makeThreadPoolExecutor(
      name,
      stopTimeout,
      minThreads,
      maxThreads,
      maxWaiters)
  }
}

trait TServer extends (thrift.TProcessor => thrift.server.TServer) {
  def name: String
  def port: Int
  var timeout     = 100.milliseconds
  var idleTimeout = 60.seconds
  var threadPool  = new ThreadPool

  def getPool = threadPool(name + "_thread_pool")

  def apply(processor: thrift.TProcessor): thrift.server.TServer
}

trait TSelectorServer extends TServer {
  def apply(processor: thrift.TProcessor) = {
    dist.thrift.TSelectorServer(name, port, processor, getPool, timeout, idleTimeout)
  }
}

/*
trait TThreadServer extends TServer {
  def apply(processor: thrift.TProcessor) = {
    gizzard.thrift.TThreadServer(name, port, idleTimeout.inMillis.toInt, getPool, processor)
  }
}*/

trait THsHaServer extends TServer {
  def apply(processor: thrift.TProcessor) = {
    val transport = new thrift.transport.TNonblockingServerSocket(port, timeout.inMillis.toInt)
    val options   = new thrift.server.TNonblockingServer.Options
    new thrift.server.THsHaServer(
      new thrift.TProcessorFactory(processor),
      transport,
      new thrift.transport.TFramedTransport.Factory(),
      new thrift.protocol.TBinaryProtocol.Factory(),
      new thrift.protocol.TBinaryProtocol.Factory(),
      getPool,
      options)
  }
}
