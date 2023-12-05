package kr.ac.postech.paranode.e2e

import kr.ac.postech.paranode.master.Master
import org.scalatest.flatspec.AnyFlatSpec
import kr.ac.postech.paranode.utils.Hooks
import kr.ac.postech.paranode.worker.Worker
import kr.ac.postech.paranode.core.Record

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.reflect.io.Path

class SystemSpec extends AnyFlatSpec {
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

    master.run()

    master.blockUntilRunning()

    workers.foreach(_.run())

    val registeredWorkers = master.blockUntilRegistered()

    val workersInRegisteredOrder =
      registeredWorkers.map(x => workers.find(_.port == x.port).get)

    val outputDirectoriesInRegisteredOrder =
      workersInRegisteredOrder.map(_.outputDirectory)

    master.blockUntilFinished()

    val outputSources = outputDirectoriesInRegisteredOrder
      .flatMap(_.toDirectory.list)
      .map(x => Source.fromURI(x.toURI))

    val outputBytes = LazyList.from(outputSources.flatMap(_.map(_.toByte)))

    val outputRecords = Record.fromBytesToRecords(outputBytes)

    assert(
      outputRecords.length == numberOfWorkers * 1000
    ) // FIXME: 1000 is hardcoded
    assert(outputRecords.isSorted)
  }

  private def inputDirectories(index: Int) = {
    val path: Path = Path(getClass.getResource(s"/data/$index").getPath)

    assert(path.exists && path.isDirectory, "Input directories must exist.")

    path.toDirectory.list.map(_.toDirectory).toArray
  }

  implicit class Records(records: LazyList[Record]) {
    def isSorted: Boolean = records match {
      case LazyList()  => true
      case LazyList(_) => true
      case _ =>
        records
          .sliding(2)
          .forall({ case LazyList(record1, record2) =>
            record1.key <= record2.key
          })
    }

  }

}
