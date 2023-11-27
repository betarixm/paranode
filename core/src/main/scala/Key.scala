package kr.ac.postech.paranode.core

import com.google.protobuf.ByteString

object Key {
  def fromString(string: String): Key = new Key(string.getBytes())

  def fromByteString(byteString: ByteString): Key = new Key(
    byteString.toByteArray
  )
}

class Key(val underlying: Array[Byte]) extends AnyVal with Ordered[Key] {
  def is(that: Key): Boolean = underlying sameElements that.underlying

  def hex: String = underlying.map("%02x" format _).mkString

  override def compare(that: Key): Int = underlying
    .zip(that.underlying)
    .map { case (a, b) =>
      a - b
    }
    .find(_ != 0)
    .getOrElse(0)

}
