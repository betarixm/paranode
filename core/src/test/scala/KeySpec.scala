package kr.ac.postech.paranode.core

import org.scalatest.flatspec.AnyFlatSpec

class KeySpec extends AnyFlatSpec {
  "Keys" should "be same if elements are same" in {
    val key = new Key(Array(0x01, 0x03, 0x03, 0x07) map (_.toByte))
    val keyWithSameElements =
      new Key(Array(0x01, 0x03, 0x03, 0x07) map (_.toByte))
    val keyWithDifferentElements =
      new Key(Array(0xde, 0xad, 0xbe, 0xef) map (_.toByte))

    assert(key is keyWithSameElements)
    assert(!(key is keyWithDifferentElements))
  }

  they should "be same if elements are same regardless of constructors" in {
    val keyFromString = Key.fromString("hello")
    val key = new Key(Array(0x68, 0x65, 0x6c, 0x6c, 0x6f))

    assert(keyFromString is key)
  }

  they should "be compare right" in {
    val key1 = new Key(Array(0x1,0x2,0x3,0x4))
    val key2 = new Key(Array(0x1,0x2,0x3,0x4))
    val key3 = new Key(Array(0x5,0x2,0x3,0x4))

    assert(key1.compare(key2) == 0)
    assert(key1.compare(key3) == -1)
    assert(key3.compare(key1) == 1)
    assert(key1 < key3)
    assert(key3 > key1)
    assert(key1 is key2)

  }

}
