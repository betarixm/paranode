package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.Key
import kr.ac.postech.paranode.core.KeyRange
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterServer
import kr.ac.postech.paranode.rpc.WorkerClient
import org.apache.logging.log4j.scala.Logging

import java.net._
import scala.concurrent.ExecutionContextExecutor

object Master extends Logging {
  private def workersWithKeyRange(
      keys: List[Key],
      workers: List[WorkerMetadata]
  ): List[WorkerMetadata] =
    keys
      .sliding(
        keys.size / workers.size,
        keys.size / workers.size
      )
      .toList
      .map(keys => KeyRange.tupled(keys.head, keys.last))
      .zip(workers)
      .map { case (keyRange, worker) =>
        worker.copy(keyRange = Some(keyRange))
      }

  def main(args: Array[String]): Unit = {
    val masterArguments = new MasterArguments(args)
    val masterHost = InetAddress.getLocalHost.getHostAddress
    val masterPort = sys.env.getOrElse("MASTER_PORT", "50051").toInt

    logger.info(
      "[Master] Arguments: \n" +
        s"masterHost: $masterHost\n" +
        s"masterPort: $masterPort\n" +
        s"numberOfWorkers: ${masterArguments.numberOfWorkers}\n"
    )

    val server =
      new MasterServer(scala.concurrent.ExecutionContext.global, masterPort)

    server.start()

    println(masterHost + ":" + masterPort)

    while (server.registeredWorkers.size < masterArguments.numberOfWorkers) {
      logger.info(s"${server.registeredWorkers}")
      Thread.sleep(1000)
    }

    val workerInfo: List[WorkerMetadata] = server.registeredWorkers

    println(workerInfo.map(_.host).mkString(", "))

    val clients = workerInfo.map { worker =>
      WorkerClient(worker.host, worker.port)
    }

    implicit val requestExecutionContext: ExecutionContextExecutor =
      scala.concurrent.ExecutionContext.fromExecutor(
        java.util.concurrent.Executors.newFixedThreadPool(workerInfo.size)
      )

    logger.info(s"[Master] Clients: $clients")

    logger.info("[Master] Sample Requested")

    val sampledKeys = clients
      .sample(64)
      .flatMap(_.sampledKeys)
      .map(Key.fromByteString)

    logger.info("[Master] Sampled")

    val sortedSampledKeys = sampledKeys.sorted

    val workers = workersWithKeyRange(sortedSampledKeys, workerInfo)

    logger.info(s"[Master] Key ranges with worker: $workers")

    logger.info("[Master] Sort started")

    clients.sort()

    logger.info("[Master] Sort finished")

    logger.info("[Master] Partition started")

    clients.partition(workers)

    logger.info("[Master] Partition finished")

    logger.info("[Master] Exchange started")

    clients.exchange(workers)

    logger.info("[Master] Exchange finished")

    logger.info("[Master] Merge started")

    clients.merge()

    logger.info("[Master] Merge finished")

    server.blockUntilShutdown()
  }

}
