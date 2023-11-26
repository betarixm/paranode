package kr.ac.postech.paranode.core

import org.apache.logging.log4j.scala.Logging

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import scala.io.Source
import scala.reflect.io.Path

object Block extends Logging {
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
    logger.debug(s"[Block] Reading block from $path")

    Block.fromSource(Source.fromURI(path.toURI), keyLength, valueLength)
  }

}

class Block(val records: LazyList[Record]) extends AnyVal {
  def toChars: LazyList[Char] = records.flatMap(_.toChars)

  def writeTo(path: Path): File = {
    val file = new File(path.toString)
    val writer = new BufferedOutputStream(new FileOutputStream(file))

    try {
      toChars.foreach(writer.write(_))
      file
      // TODO: Handle exceptions
    } finally writer.close()
  }

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
