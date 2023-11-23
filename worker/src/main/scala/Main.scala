package kr.ac.postech.paranode.worker

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterClient
import scala.util.{Failure, Success}
import java.net.InetAddress
import scala.util.Try

import kr.ac.postech.paranode.rpc.master.RegisterRequest

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 5) {
      println("Usage: worker <IP:Port> -I <InputDirs> -O <OutputDir>")
      return
    }

    val Array(ip, portStr) = args(0).split(":")
    val port = Try(portStr.toInt).getOrElse {
      println("Invalid port number.")
      return
    }

    val inputDirIndex = args.indexOf("-I") + 1
    val outputDirIndex = args.indexOf("-O") + 1

    if (inputDirIndex <= 0 || outputDirIndex <= 0) {
      println("Input or Output directories not specified correctly.")
      return
    }

    args.slice(inputDirIndex, outputDirIndex - 1)
    args(outputDirIndex)

    // Open MasterClient and request register
    val client = MasterClient(ip, port)
    val ipAddress = InetAddress.getLocalHost.getHostAddress
    val workerMetadata = WorkerMetadata(ipAddress, -1, None)
    val registerReply = client.register(workerMetadata)
    registerReply.onComplete {
      case Success(_) =>
        client.shutdown()
      // TODO: start WorkerServer
      case Failure(exception) =>
        println(s"Registration failed: ${exception.getMessage}")
        client.shutdown()
        System.exit(1)
    }(scala.concurrent.ExecutionContext.global)

    // TODO: wait for SampleRequest (with sorting)

  }

}
