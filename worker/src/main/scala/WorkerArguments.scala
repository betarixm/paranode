package kr.ac.postech.paranode.worker

import scala.reflect.io.Directory
import scala.reflect.io.Path

class WorkerArguments(args: Array[String]) {
  def masterHost: String = args(0).split(":")(0)

  def masterPort: Int = args(0).split(":")(1).toInt

  def inputDirectories: Array[Directory] = {
    val directories = args
      .slice(inputDirectoriesIndex, outputDirectoryIndex - 1)
      .map(Path.string2path)
      .map(_.toDirectory)

    assert(
      directories.forall(directory =>
        directory.exists && directory.isDirectory
      ),
      "Input directories must exist."
    )

    directories
  }

  def outputDirectory: Directory = {
    val directory = Path.string2path(args(outputDirectoryIndex)).toDirectory

    assert(
      directory.exists && directory.isDirectory,
      "Output directory must exist."
    )

    directory
  }

  private def inputDirectoriesIndex = args.indexOf("-I") + 1

  private def outputDirectoryIndex = args.indexOf("-O") + 1
}
