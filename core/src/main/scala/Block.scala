package kr.ac.postech.paranode.core
import scala.io.Source
import scala.reflect.io.Path

object Block {
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
  ): Block =
    Block.fromSource(Source.fromURI(path.toURI), keyLength, valueLength)
}

class Block(val records: LazyList[Record]) extends AnyVal {
  def toChars: LazyList[Char] = records.flatMap(_.toChars)
}
