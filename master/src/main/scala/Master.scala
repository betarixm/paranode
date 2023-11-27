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

    logger.debug(
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
      logger.debug(s"${server.registeredWorkers}")
      Thread.sleep(1000)
    }

    val workerInfo: List[WorkerMetadata] = server.registeredWorkers

    println(workerInfo.map(_.host).mkString(", "))

    val clients = workerInfo.map { worker =>
      WorkerClient(worker.host, worker.port)
    }

    implicit val requestExecutionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.fromExecutor(
      java.util.concurrent.Executors.newFixedThreadPool(workerInfo.size)
    )

    val sampledKeys = clients
      .sample(64)
      .flatMap(_.sampledKeys)
      .map(Key.fromByteString)

    logger.debug(s"[Master] Sampled keys: $sampledKeys")

    val sortedSampledKeys = sampledKeys.sorted

    logger.debug(s"[Master] Sorted Sampled keys: $sortedSampledKeys")

    val workers = workersWithKeyRange(sortedSampledKeys, workerInfo)

    logger.debug(s"[Master] Key ranges with worker: $workers")

    logger.debug("[Master] Sort started")

    clients.sort()

    logger.debug("[Master] Sort finished")

    logger.debug("[Master] Partition started")

    clients.partition(workers)

    logger.debug("[Master] Partition finished")

    logger.debug("[Master] Exchange started")

    clients.exchange(workers)

    logger.debug("[Master] Exchange finished")

    logger.debug("[Master] Merge started")

    clients.merge()

    logger.debug("[Master] Merge finished")

    server.blockUntilShutdown()
  }

}
