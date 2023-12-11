package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.Key
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.GrpcServer
import kr.ac.postech.paranode.rpc.WorkerClient
import kr.ac.postech.paranode.utils.Hooks
import kr.ac.postech.paranode.utils.MutableState
import kr.ac.postech.paranode.utils.Progress
import kr.ac.postech.paranode.utils.Progress._
import org.apache.logging.log4j.scala.Logging

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

object Master extends Logging {
  def main(args: Array[String]): Unit = {
    val masterArguments = new MasterArguments(args)
    val masterHost = Hooks.useLocalHostAddress
    val masterPort = sys.env.getOrElse("MASTER_PORT", "50051").toInt

    val master = new Master(
      masterHost,
      masterPort,
      masterArguments.numberOfWorkers
    )

    val executor = Executors.newCachedThreadPool()

    val executionContext: ExecutionContext =
      ExecutionContext.fromExecutor(executor)

    Await.result(
      master.run()(executionContext),
      scala.concurrent.duration.Duration.Inf
    )

    executor.shutdown()
    executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
  }

}

class Master(host: String, port: Int, numberOfWorkers: Int) extends Logging {
  private val serverState: MutableState[Progress] =
    new MutableState[Progress](Progress.Idle)

  private val registerState: MutableState[Progress] =
    new MutableState[Progress](Progress.Idle)

  private val mutableWorkers = new MutableState[List[WorkerMetadata]](Nil)

  def run()(implicit executionContext: ExecutionContext): Future[Unit] =
    Future {
      logger.info(
        "[Master] Arguments: \n" +
          s"masterHost: $host\n" +
          s"masterPort: $port\n" +
          s"numberOfWorkers: ${numberOfWorkers}\n"
      )

      val serviceExecutor = Executors.newCachedThreadPool()

      val serviceExecutionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(serviceExecutor)

      val server =
        new GrpcServer(
          MasterService(mutableWorkers)(serviceExecutionContext),
          port
        )

      server.start()

      println(host + ":" + port)

      serverState.update(_ => Progress.Running)
      registerState.update(_ => Progress.Running)

      while (mutableWorkers.get.size < numberOfWorkers) {
        logger.info(s"${mutableWorkers.get}")
        Thread.sleep(1000)
      }

      val registeredWorkers: List[WorkerMetadata] = mutableWorkers.get

      registerState.update(_ => Progress.Finished)

      println(registeredWorkers.map(_.host).mkString(", "))

      val clients = registeredWorkers.map { worker =>
        WorkerClient(worker.host, worker.port)
      }

      val requestExecutor = Executors
        .newFixedThreadPool(registeredWorkers.size)

      val requestExecutionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(
          requestExecutor
        )

      logger.info(s"[Master] Clients: $clients")

      logger.info("[Master] Sample Requested")

      val sampledKeys = clients
        .sample(1024)(requestExecutionContext)
        .flatMap(_.sampledKeys)
        .map(Key.fromByteString)

      logger.info("[Master] Sampled")

      val workers = sampledKeys
        .rangesBy(registeredWorkers.size, Key.min(), Key.max())
        .map(Some(_))
        .zip(registeredWorkers)
        .map { case (keyRange, worker) =>
          worker.copy(keyRange = keyRange)
        }

      logger.info(s"[Master] Key ranges with worker: $workers")

      logger.info("[Master] Sort started")

      clients.sort()(requestExecutionContext)

      logger.info("[Master] Sort finished")

      logger.info("[Master] Partition started")

      clients.partition(workers)(requestExecutionContext)

      logger.info("[Master] Partition finished")

      logger.info("[Master] Exchange started")

      clients.exchange(workers)(requestExecutionContext)

      logger.info("[Master] Exchange finished")

      logger.info("[Master] Merge started")

      clients.merge()(requestExecutionContext)

      logger.info("[Master] Merge finished")

      logger.info("[Master] Terminate started")

      clients.terminate()(requestExecutionContext)

      logger.info("[Master] Terminate finished")

      clients.foreach(_.shutdown())

      server.stop()

      serverState.update(_ => Progress.Finished)

      serviceExecutor.shutdown()
      requestExecutor.shutdown()

      serviceExecutor.awaitTermination(
        5,
        TimeUnit.SECONDS
      )
      requestExecutor.awaitTermination(
        5,
        TimeUnit.SECONDS
      )
    }

  def blockUntilRunning(): Unit = {
    while (!isRunning) {
      Thread.sleep(1000)
    }
  }

  def blockUntilFinished(): Unit = {
    while (!isFinished) {
      Thread.sleep(1000)
    }
  }

  def blockUntilRegistered(): List[WorkerMetadata] = {
    while (!isRegistered) {
      Thread.sleep(1000)
    }
    mutableWorkers.get
  }

  def isRunning: Boolean = serverState.get == Progress.Running

  def isRegistered: Boolean = registerState.get == Progress.Finished

  def isFinished: Boolean = serverState.get == Progress.Finished
}
