package kr.ac.postech.paranode.core

object Key {
  def fromString(string: String): Key = new Key(string.getBytes())
}

class Key(val underlying: Array[Byte]) extends AnyVal with Ordered[Key] {
  def is(that: Key): Boolean = underlying sameElements that.underlying

  override def compare(that: Key): Int = {
    val result = underlying
      .zip(that.underlying)
      .map { case (a, b) =>
        a - b
      }
      .find(_ != 0)
    result.getOrElse(0)
  }
}
