package kr.ac.postech.paranode.master

import kr.ac.postech.paranode.core.WorkerMetadata

import java.nio.file.Files
import java.nio.file.Paths
import scala.collection.mutable.ListBuffer
import scala.io.Source

object AuxFunction {

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

}
