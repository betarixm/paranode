package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.WorkerMetadata
import kr.ac.postech.paranode.rpc.MasterServer

import scala.util.Try
import scala.io.Source
import java.net.URL
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ListBuffer

object Main {
  def main(args: Array[String]): Unit = {
    val requestLimit = Try(args(1).toInt).getOrElse {
      println("Invalid command")
      return
    }

    val server = new MasterServer(scala.concurrent.ExecutionContext.global)
    server.startServer()

    while (server.getRequestCount < requestLimit) {
      Thread.sleep(1000)
    }

    val workerInfo: List[WorkerMetadata] = getWorkerDetails()

    assert(workerInfo.size == requestLimit)

    try {
      val url = new URL("http://checkip.amazonaws.com")
      val source = Source.fromURL(url)
      val publicIpAddress = source.mkString.trim
      source.close()

      println(publicIpAddress + ":" + server.getPort)
      println(workerInfo.map(_.host).mkString(", "))
    } catch {
      case e: Exception => e.printStackTrace()
    }

    // TODO: start WorkerClient

  }
}

def getWorkerDetails(): List[WorkerMetadata] = {

  val dirPath = Paths.get("worker_register")
  val workerDetails = ListBuffer[WorkerMetadata]()

  if (Files.exists(dirPath)) {
    val files = Files.list(dirPath).toArray
    files.foreach { file =>
      val source = Source.fromFile(file.toString)
      try {
        for (line <- source.getLines) {
          val parts = line.split(", ").map(_.split(": ").last)
          if (parts.length == 2) {
            val ip = parts(0)
            val port = parts(1).toInt
            workerDetails += (WorkerMetadata(ip, port, None))
          }
        }
      } finally {
        source.close()
      }
    }
  }

  workerDetails.toList
}
