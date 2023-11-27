package kr.ac.postech.paranode.core

import org.apache.logging.log4j.scala.Logging

import scala.io.Source
import scala.reflect.io.Directory
import scala.reflect.io.File
import scala.reflect.io.Path

object Block extends Logging {

  implicit class Blocks(blocks: List[Block]) {
    def merged: Block = new Block(Record.merged(blocks.map(_.records)))
  }

  def fromBytes(
      bytes: LazyList[Byte],
      keyLength: Int = 10,
      valueLength: Int = 90
  ): Block = new Block(Record.fromBytesToRecords(bytes, keyLength, valueLength))

  def fromSource(
      source: Source,
      keyLength: Int = 10,
      valueLength: Int = 90
  ): Block =
    Block.fromBytes(
      LazyList.from(source.map(_.toByte)),
      keyLength,
      valueLength
    )

  def fromPath(
      path: Path,
      keyLength: Int = 10,
      valueLength: Int = 90
  ): Block = {
    logger.info(s"[Block] Reading block from $path")

    Block.fromSource(Source.fromURI(path.toURI), keyLength, valueLength)
  }

}

class Block(val records: LazyList[Record]) extends AnyVal {
  def toChars: LazyList[Char] = records.flatMap(_.toChars)

  def writeTo(path: Path): File = {
    val file = File(path)
    val writer = file.bufferedOutput()

    try {
      toChars.foreach(writer.write(_))
      file
      // TODO: Handle exceptions
    } finally writer.close()
  }

  def writeToDirectory(
      directory: Directory,
      size: Int = 320000
  ): List[File] =
    records
      .grouped(size)
      .zipWithIndex
      .map({ case (records, index) =>
        val file = File(directory / s"partition.$index")
        val writer = file.bufferedOutput()

        try {
          records.foreach(_.toChars.foreach(writer.write(_)))
          file
        } finally writer.close()

      })
      .toList

  def filterByKeyRange(keyRange: KeyRange): Block = new Block(
    records.filter(keyRange.includes)
  )

  def partition(keyRange: KeyRange): Partition =
    (keyRange, filterByKeyRange(keyRange))

  def partition(keyRanges: List[KeyRange]): List[Partition] =
    keyRanges.map(partition)

  def sorted: Block =
    new Block(records.sortBy(_.key))

  def sample(number: Int = 64): LazyList[Key] =
    Record.sample(records, number)

}
