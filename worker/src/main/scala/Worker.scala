package kr.ac.postech.paranode.worker

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.GrpcServer
import kr.ac.postech.paranode.rpc.MasterClient
import kr.ac.postech.paranode.utils.Hooks
import org.apache.logging.log4j.scala.Logging

import java.util.concurrent.Executors
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.io.Directory

object Worker extends Logging {

  def main(args: Array[String]): Unit = {
    val workerArguments = new WorkerArguments(args)
    val workerHost = Hooks.useLocalHostAddress
    val workerPort = Hooks.useAvailablePort

    val worker = new Worker(
      workerHost,
      workerPort,
      workerArguments.masterHost,
      workerArguments.masterPort,
      workerArguments.inputDirectories,
      workerArguments.outputDirectory
    )

    Await.result(
      worker.run()(ExecutionContext.global),
      scala.concurrent.duration.Duration.Inf
    )
  }

}

class Worker(
    host: String,
    port: Int,
    masterHost: String,
    masterPort: Int,
    inputDirectories: Array[Directory],
    outputDirectory: Directory
) extends Logging {
  def run()(implicit executionContext: ExecutionContext): Future[Unit] =
    Future {
      logger.info(
        "[Worker] Arguments: \n" +
          s"workerHost: $host\n" +
          s"workerPort: $port\n" +
          s"masterIp: $masterHost\n" +
          s"masterPort: $masterPort\n" +
          s"inputDirectories: ${inputDirectories.mkString(", ")}\n" +
          s"outputDirectory: $outputDirectory\n"
      )

      val workerMetadata = WorkerMetadata(host, port, None)

      val serviceExecutionContext: ExecutionContext =
        ExecutionContext.fromExecutor(
          Executors.newCachedThreadPool()
        )

      val server = new GrpcServer(
        WorkerService(
          inputDirectories,
          outputDirectory
        )(serviceExecutionContext),
        port
      )

      val client =
        MasterClient(masterHost, masterPort)

      server.start()

      Await.result(
        client.register(workerMetadata),
        scala.concurrent.duration.Duration.Inf
      )

      client.shutdown()

      server.blockUntilShutdown()
    }
}
