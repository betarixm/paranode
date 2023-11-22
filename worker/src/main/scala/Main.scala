package kr.ac.postech.paranode.worker

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterClient

import java.net.URL
import scala.io.Source
import scala.util.Try

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
    try {
      val url = new URL("http://checkip.amazonaws.com")
      val source = Source.fromURL(url)
      val publicIpAddress = source.mkString.trim
      source.close()

      val workerMetadata = WorkerMetadata(publicIpAddress, -1, None)
      client.register(workerMetadata)

    } finally {
      client.shutdown()
    }

  }

}
