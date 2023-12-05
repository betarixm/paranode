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

    Await.result(
      master.run()(ExecutionContext.global),
      scala.concurrent.duration.Duration.Inf
    )
  }

}

class Master(host: String, port: Int, numberOfWorkers: Int) extends Logging {
  private val state: MutableState[Progress] =
    new MutableState[Progress](Progress.Idle)

  def run()(implicit executionContext: ExecutionContext): Future[Unit] =
    Future {
      logger.info(
        "[Master] Arguments: \n" +
          s"masterHost: $host\n" +
          s"masterPort: $port\n" +
          s"numberOfWorkers: ${numberOfWorkers}\n"
      )

      val serviceExecutionContext: ExecutionContextExecutor =
        ExecutionContext.fromExecutor(
          Executors.newCachedThreadPool()
        )

      val mutableWorkers = new MutableState[List[WorkerMetadata]](Nil)

      val server =
        new GrpcServer(
          MasterService(mutableWorkers)(serviceExecutionContext),
          port
        )

      server.start()

      println(host + ":" + port)

      state.update(_ => Progress.Running)

      while (mutableWorkers.get.size < numberOfWorkers) {
        logger.info(s"${mutableWorkers.get}")
        Thread.sleep(1000)
      }

      val registeredWorkers: List[WorkerMetadata] = mutableWorkers.get

      println(registeredWorkers.map(_.host).mkString(", "))

      val clients = registeredWorkers.map { worker =>
        WorkerClient(worker.host, worker.port)
      }

      val requestExecutionContext: ExecutionContextExecutor =
        scala.concurrent.ExecutionContext.fromExecutor(
          java.util.concurrent.Executors
            .newFixedThreadPool(registeredWorkers.size)
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

      clients.foreach(_.shutdown())

      server.stop()

      state.update(_ => Progress.Finished)
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

  def isRunning: Boolean = state.get == Progress.Running

  def isFinished: Boolean = state.get == Progress.Finished
}
