package kr.ac.postech.paranode.worker

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.GrpcServer
import kr.ac.postech.paranode.rpc.MasterClient
import kr.ac.postech.paranode.utils.Hooks
import org.apache.logging.log4j.scala.Logging

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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

    val executor = Executors.newCachedThreadPool()

    val executionContext: ExecutionContext =
      ExecutionContext.fromExecutor(
        executor
      )

    try {
      Await.result(
        worker.run()(executionContext),
        scala.concurrent.duration.Duration.Inf
      )
    } catch {
      case statusRuntimeException: io.grpc.StatusRuntimeException if {
            val status = statusRuntimeException.getStatus
            status.getCode == io.grpc.Status.Code.UNAVAILABLE && status.getDescription
              .contains("debug data: app_requested")
          } => // Suppress
    } finally {
      worker.shutdown()
      executor.shutdown()
    }
  }

}

class Worker(
    val host: String,
    val port: Int,
    val masterHost: String,
    val masterPort: Int,
    val inputDirectories: Array[Directory],
    val outputDirectory: Directory
) extends Logging {
  private val workerMetadata = WorkerMetadata(host, port, None)

  private val serviceExecutor = Executors.newCachedThreadPool()

  private val serviceExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(
      serviceExecutor
    )

  private val server = new GrpcServer(
    WorkerService(
      inputDirectories,
      outputDirectory,
      onFinished = () => {
        serviceExecutor.shutdown()
        serviceExecutor.awaitTermination(3, TimeUnit.SECONDS)
      }
    )(serviceExecutionContext),
    port
  )

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

  def shutdown(): Unit = {
    server.stop()
  }
}
