package kr.ac.postech.paranode.worker

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterClient
import kr.ac.postech.paranode.rpc.WorkerServer
import org.apache.logging.log4j.scala.Logging

import java.net.InetAddress
import java.net.ServerSocket
import scala.concurrent.Await
import scala.reflect.io.Path
import scala.util.Using

object Worker extends Logging {
  private class WorkerArguments(args: Array[String]) {
    def masterIp: String = args(0).split(":")(0)

    def masterPort: Int = args(0).split(":")(1).toInt

    def inputDirectories(): Array[Path] =
      args
        .slice(inputDirectoriesIndex, outputDirectoryIndex - 1)
        .map(Path.string2path)

    def outputDirectory(): Path = Path.string2path(args(outputDirectoryIndex))

    private def inputDirectoriesIndex = args.indexOf("-I") + 1

    private def outputDirectoryIndex = args.indexOf("-O") + 1
  }

  def main(args: Array[String]): Unit = {
    val workerArguments = new WorkerArguments(args)

    logger.debug(
      "Worker arguments: \n" +
        s"masterIp: ${workerArguments.masterIp}\n" +
        s"masterPort: ${workerArguments.masterPort}\n" +
        s"inputDirectories: ${workerArguments.inputDirectories().mkString(", ")}\n" +
        s"outputDirectory: ${workerArguments.outputDirectory()}\n"
    )

    val workerPort = Using(new ServerSocket(0))(_.getLocalPort).get

    val server = new WorkerServer(
      scala.concurrent.ExecutionContext.global,
      workerPort,
      workerArguments.inputDirectories(),
      workerArguments.outputDirectory()
    )

    server.start()

    val client =
      MasterClient(workerArguments.masterIp, workerArguments.masterPort)

    try {
      val ipAddress = InetAddress.getLocalHost.getHostAddress
      val workerMetadata = WorkerMetadata(ipAddress, workerPort, None)

      Await.result(
        client.register(workerMetadata),
        scala.concurrent.duration.Duration.Inf
      )

    } finally {
      client.shutdown()
    }

    server.blockUntilShutdown()
  }

}
