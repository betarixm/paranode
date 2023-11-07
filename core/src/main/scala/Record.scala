package kr.ac.postech.paranode.core

object Record {
  def fromString(string: String, keyLength: Int = 10): Record =
    Record.fromBytes(string.getBytes(), keyLength)

  def fromBytes(bytes: Array[Byte], keyLength: Int = 10): Record = {
    val (rawKey, value) = bytes.splitAt(keyLength)
    new Record(new Key(rawKey), value)
  }
}

class Record(val key: Key, val value: Array[Byte]) extends Ordered[Record] {
  def is(that: Record): Boolean =
    (key is that.key) && (value sameElements that.value)

  override def compare(that: Record): Int = key.compare(that.key)
}
