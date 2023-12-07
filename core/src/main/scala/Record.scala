package kr.ac.postech.paranode.core

import org.apache.logging.log4j.scala.Logging

import scala.io.Source
import scala.reflect.io.Directory

object Record extends Logging {
  def fromString(string: String, keyLength: Int = 10): Record =
    Record.fromBytes(string.getBytes(), keyLength)

  def fromBytes(bytes: Array[Byte], keyLength: Int = 10): Record = {
    val (rawKey, value) = bytes.splitAt(keyLength)
    new Record(new Key(rawKey), value)
  }

  def fromBytesToRecords(
      bytes: LazyList[Byte],
      keyLength: Int = 10,
      valueLength: Int = 90
  ): LazyList[Record] = {
    if (bytes.isEmpty) {
      LazyList.empty
    } else {
      val recordLength = keyLength + valueLength
      val (head, tail) = bytes.splitAt(recordLength)

      Record.fromBytes(head.toArray, keyLength) #:: Record
        .fromBytesToRecords(
          tail,
          keyLength,
          valueLength
        )
    }
  }

  def fromDirectories(
      directories: List[Directory],
      keyLength: Int = 10,
      valueLength: Int = 90
  ): LazyList[Record] = {
    val sources = directories
      .flatMap(_.files)
      .map(file => Source.fromURI(file.toURI))

    val bytes = LazyList.from(sources.flatMap(_.map(_.toByte)))

    Record.fromBytesToRecords(bytes, keyLength, valueLength)
  }

  def sample(
      records: LazyList[Record],
      number: Int = 64
  ): LazyList[Key] = records.take(number).map(_.key)

  def merged(
      listOfRecords: List[LazyList[Record]]
  ): LazyList[Record] = {
    if (listOfRecords.isEmpty) {
      LazyList.empty
    } else {
      val sortedListOfRecords =
        listOfRecords.sorted(Ordering.by((_: LazyList[Record]).head.key))

      sortedListOfRecords.head.head #:: merged(
        (sortedListOfRecords.head.tail :: sortedListOfRecords.tail).filter(
          _.nonEmpty
        )
      )
    }
  }
}

class Record(val key: Key, val value: Array[Byte]) extends Ordered[Record] {
  def is(that: Record): Boolean =
    (key is that.key) && (value sameElements that.value)

  def toChars: Array[Char] = key.underlying.map(_.toChar) ++ value.map(_.toChar)

  override def compare(that: Record): Int = key.compare(that.key)
}
