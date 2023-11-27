package kr.ac.postech.paranode.core

import com.google.protobuf.ByteString

object Key {
  def fromString(string: String): Key = new Key(string.getBytes())

  def fromByteString(byteString: ByteString): Key = new Key(
    byteString.toByteArray
  )

  def min(size: Int = 10) = new Key(Array.fill[Byte](size)(0x00.toByte))

  def max(size: Int = 10) = new Key(Array.fill[Byte](size)(0xff.toByte))
}

case class Key(underlying: Array[Byte]) extends AnyVal with Ordered[Key] {
  def is(that: Key): Boolean = underlying sameElements that.underlying

  def hex: String = underlying.map("%02x" format _).mkString

  def prior: Key = new Key(underlying.init :+ (underlying.last - 1).toByte)

  override def compare(that: Key): Int = underlying
    .zip(that.underlying)
    .map { case (a, b) =>
      a.toChar - b.toChar
    }
    .find(_ != 0)
    .getOrElse(0)

}
