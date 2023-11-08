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

  they should "be comparable" in {
    val key1 = new Key(Array(0x01, 0x03, 0x03, 0x07).map(_.toByte))
    val key2 = new Key(Array(0x01, 0x03, 0x03, 0x08).map(_.toByte))
    val key3 = new Key(Array(0x01, 0x03, 0x03, 0x09).map(_.toByte))
    val key4 = new Key(Array(0x01, 0x03, 0x03, 0x07).map(_.toByte))
    val key5 = new Key(Array(0x01, 0x03, 0x01, 0x02).map(_.toByte))

    assert(key1.compare(key2) == -1)
    assert(key1.compare(key3) == -2)
    assert(key1.compare(key4) == 0)
    assert(key1.compare(key5) == 2)
  }
}
