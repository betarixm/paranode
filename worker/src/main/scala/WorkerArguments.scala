package kr.ac.postech.paranode.worker

import scala.reflect.io.Directory
import scala.reflect.io.Path

class WorkerArguments(args: Array[String]) {
  def masterHost: String = args(0).split(":")(0)

  def masterPort: Int = args(0).split(":")(1).toInt

  def inputDirectories: Array[Directory] =
    args
      .slice(inputDirectoriesIndex, outputDirectoryIndex - 1)
      .map(Path.string2path)
      .map(_.toDirectory)

  def outputDirectory: Directory =
    Path.string2path(args(outputDirectoryIndex)).toDirectory

  private def inputDirectoriesIndex = args.indexOf("-I") + 1

  private def outputDirectoryIndex = args.indexOf("-O") + 1
}
