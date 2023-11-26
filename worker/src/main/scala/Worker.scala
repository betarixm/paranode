package kr.ac.postech.paranode.worker

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterClient
import kr.ac.postech.paranode.rpc.WorkerServer
import org.apache.logging.log4j.scala.Logging

import java.net.InetAddress
import java.net.ServerSocket
import scala.concurrent.Await
import scala.util.Using

object Worker extends Logging {

  def main(args: Array[String]): Unit = {
    val workerArguments = new WorkerArguments(args)
    val workerHost = InetAddress.getLocalHost.getHostAddress
    val workerPort = Using(new ServerSocket(0))(_.getLocalPort).get
    val workerMetadata = WorkerMetadata(workerHost, workerPort, None)

    logger.debug(
      "[Worker] Arguments: \n" +
        s"workerHost: $workerHost\n" +
        s"workerPort: $workerPort\n" +
        s"masterIp: ${workerArguments.masterHost}\n" +
        s"masterPort: ${workerArguments.masterPort}\n" +
        s"inputDirectories: ${workerArguments.inputDirectories.mkString(", ")}\n" +
        s"outputDirectory: ${workerArguments.outputDirectory}\n"
    )

    val server = new WorkerServer(
      scala.concurrent.ExecutionContext.global,
      workerPort,
      workerArguments.inputDirectories,
      workerArguments.outputDirectory
    )

    val client =
      MasterClient(workerArguments.masterHost, workerArguments.masterPort)

    server.start()

    Await.result(
      client.register(workerMetadata),
      scala.concurrent.duration.Duration.Inf
    )

    client.shutdown()

    server.blockUntilShutdown()
  }

}
