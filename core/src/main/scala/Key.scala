package kr.ac.postech.paranode.core

object Key {
  def fromString(string: String): Key = new Key(string.getBytes())
}

class Key(val underlying: Array[Byte]) extends AnyVal with Ordered[Key] {
  def is(that: Key): Boolean = underlying sameElements that.underlying

  override def compare(that: Key): Int = {
    // TODO: Implement compare
    underlying.length
  }
}
