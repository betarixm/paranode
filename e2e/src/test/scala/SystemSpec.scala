package kr.ac.postech.paranode.e2e

import kr.ac.postech.paranode.master.Master
import org.scalatest.flatspec.AnyFlatSpec
import kr.ac.postech.paranode.utils.Hooks
import kr.ac.postech.paranode.worker.Worker
import kr.ac.postech.paranode.core.Record
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.io.Path

class SystemSpec extends AnyFlatSpec with Logging {
  private val masterPort: Int = Hooks.useAvailablePort

  "System" should "be working" in {
    val numberOfWorkers = 3

    val outputDirectories =
      (0 until numberOfWorkers).map(_ => Hooks.useTemporaryDirectory)

    val master =
      new Master(Hooks.useLocalHostAddress, masterPort, numberOfWorkers)

    val workers =
      outputDirectories.zipWithIndex.map({ case (outputDirectory, index) =>
        new Worker(
          Hooks.useLocalHostAddress,
          Hooks.useAvailablePort,
          Hooks.useLocalHostAddress,
          masterPort,
          inputDirectories(index),
          outputDirectory
        )
      })

    val expectedOutputRecords =
      Record
        .fromDirectories(
          (0 until numberOfWorkers).flatMap(inputDirectories).toList
        )
        .sorted
        .toList

    master.run()

    master.blockUntilRunning()

    workers.foreach(_.run())

    val registeredWorkers = master.blockUntilRegistered()

    val workersInRegisteredOrder =
      registeredWorkers.map(x => workers.find(_.port == x.port).get)

    val outputDirectoriesInRegisteredOrder =
      workersInRegisteredOrder.map(_.outputDirectory)

    master.blockUntilFinished()

    val outputRecords =
      Record.fromDirectories(outputDirectoriesInRegisteredOrder).toList

    workers.foreach(_.shutdown())

    logger.info(
      s"[SystemSpec] Expected Output Records: ${expectedOutputRecords.length}"
    )
    logger.info(s"[SystemSpec] Output Records: ${outputRecords.length}")

    assert(
      outputRecords is expectedOutputRecords
    )

  }

  implicit class Records(val records: List[Record]) {
    def is(that: List[Record]): Boolean = {
      records.length == that.length && records
        .zip(that)
        .forall(records => records._1 is records._2)
    }
  }

  private def inputDirectories(index: Int) = {
    val path: Path = Path(getClass.getResource(s"/data/$index").getPath)

    assert(path.exists && path.isDirectory, "Input directories must exist.")

    path.toDirectory.list.map(_.toDirectory).toArray
  }

}
