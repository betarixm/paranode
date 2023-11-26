package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.Key
import kr.ac.postech.paranode.core.KeyRange
import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterServer
import kr.ac.postech.paranode.rpc.WorkerClient
import org.apache.logging.log4j.scala.Logging

import java.net._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Master extends Logging {
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

    println(masterHost + ":" + server.port)

    while (server.getWorkerDetails.size < masterArguments.numberOfWorkers) {
      Thread.sleep(1000)
    }

    val workerInfo: List[WorkerMetadata] = server.getWorkerDetails

    println(workerInfo.map(_.host).mkString(", "))

    val clients = workerInfo.map { worker =>
      WorkerClient(worker.host, worker.port)
    }

    val sampledKeys = Await
      .result(
        Future.sequence(clients.map(_.sample(64))),
        scala.concurrent.duration.Duration.Inf
      )
      .flatMap(_.sampledKeys)
      .map(Key.fromByteString)

    logger.debug(s"[Master] Sampled keys: $sampledKeys")

    val sortedSampledKeys = sampledKeys.sorted

    logger.debug(s"[Master] Sorted Sampled keys: $sortedSampledKeys")

    val keyRanges = sortedSampledKeys
      .sliding(
        sortedSampledKeys.size / masterArguments.numberOfWorkers,
        sortedSampledKeys.size / masterArguments.numberOfWorkers
      )
      .toList
      .map(keys => (keys.head, keys.last))

    val keyRangesWithWorker = workerInfo.zip(keyRanges.map(KeyRange.tupled))

    logger.debug(s"[Master] Key ranges with worker: $keyRangesWithWorker")

    logger.debug("[Master] Sort started")

    Await.result(
      Future.sequence(
        clients.map(_.sort())
      ),
      scala.concurrent.duration.Duration.Inf
    )

    logger.debug("[Master] Sort finished")

    logger.debug("[Master] Partition started")

    Await.result(
      Future.sequence(
        clients.map(_.partition(keyRangesWithWorker))
      ),
      scala.concurrent.duration.Duration.Inf
    )

    logger.debug("[Master] Partition finished")

    server.blockUntilShutdown()
  }

}
