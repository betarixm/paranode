package kr.ac.postech.paranode.core

object Block {
  def fromBytes(
      bytes: LazyList[Byte],
      keyLength: Int = 10,
      valueLength: Int = 90
  ): Block = new Block(Record.fromBytesToRecords(bytes, keyLength, valueLength))
}

class Block(val records: LazyList[Record]) extends AnyVal {}
