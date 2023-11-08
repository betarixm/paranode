package kr.ac.postech.paranode.core

object Key {
  def fromString(string: String): Key = new Key(string.getBytes())
}

class Key(val underlying: Array[Byte]) extends AnyVal with Ordered[Key] {
  def is(that: Key): Boolean = underlying sameElements that.underlying

  override def compare(that: Key): Int = {

    def toInt(boolean: Boolean): Int = if (boolean) 1 else -1

    def compareByte(a: Byte, b:Byte) = if (a==b) 0 else toInt(a>b)

    def compareInner(key1:List[Byte], key2:List[Byte]):Int = {
      if (key1.length == 0) 0
      else
      {
        if (key1.head == key2.head) compareInner(key1.tail, key2.tail)
        else compareByte(key1.head, key2.head)
      }
    }

    compareInner(underlying.toList, that.underlying.toList)
  }
}
