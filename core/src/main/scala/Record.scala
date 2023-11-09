package kr.ac.postech.paranode.core

object Record {
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
    val recordLength = keyLength + valueLength
    val (head, tail) = bytes.splitAt(recordLength)

    Record.fromBytes(head.toArray, keyLength) #:: Record
      .fromBytesToRecords(
        tail,
        keyLength,
        valueLength
      )
  }

  def sampleWithInterval(
      records: LazyList[Record],
      interval: Int = 10
  ): LazyList[Key] = {
    if (records.isEmpty)
      LazyList.empty[Key]
    else {
      val (current, rest) = records.splitAt(interval)
      val head = current.head.key
      head #:: sampleWithInterval(rest, interval)
    }
  }
}

class Record(val key: Key, val value: Array[Byte]) extends Ordered[Record] {
  def is(that: Record): Boolean =
    (key is that.key) && (value sameElements that.value)

  def toChars: Array[Char] = key.underlying.map(_.toChar) ++ value.map(_.toChar)

  override def compare(that: Record): Int = key.compare(that.key)
}
