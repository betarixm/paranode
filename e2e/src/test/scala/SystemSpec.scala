package kr.ac.postech.paranode.e2e

import kr.ac.postech.paranode.master.Master
import org.scalatest.flatspec.AnyFlatSpec
import kr.ac.postech.paranode.utils.Hooks
import kr.ac.postech.paranode.worker.Worker

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.io.Path

class SystemSpec extends AnyFlatSpec {
  private val masterPort: Int = Hooks.useAvailablePort

  "System" should "be working" in {
    val numberOfWorkers = 2

    val workerOutputDirectories =
      (0 until numberOfWorkers).map(_ => Hooks.useTemporaryDirectory)

    val master =
      new Master(Hooks.useLocalHostAddress, masterPort, numberOfWorkers)

    val workers = workerOutputDirectories.zipWithIndex.map({
      case (outputDirectory, index) =>
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

    master.blockUntilFinished()
  }

  private def inputDirectories(index: Int) = {
    val path: Path = Path(getClass.getResource(s"/data/$index").getPath)

    assert(path.exists && path.isDirectory, "Input directories must exist.")

    path.toDirectory.list.map(_.toDirectory).toArray
  }

}
