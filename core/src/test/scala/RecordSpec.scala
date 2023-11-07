package kr.ac.postech.paranode.core

import org.scalatest.flatspec.AnyFlatSpec

class RecordSpec extends AnyFlatSpec {
  "Records" should "be same if elements are same" in {
    val record = new Record(new Key(Array(0x1, 0x2)), Array(0x3, 0x4))
    val recordWithSameMembers =
      new Record(new Key(Array(0x1, 0x2)), Array(0x3, 0x4))
    val recordWithDifferentMembers =
      new Record(new Key(Array(0x1, 0x2)), Array(0x3, 0x5))

    assert(record is recordWithSameMembers)
    assert(!(record is recordWithDifferentMembers))
  }

  they should "be same if elements are same regardless of constructors" in {
    val recordFromString = Record.fromString("BEEF", 1)
    val recordFromBytes = Record.fromBytes(Array(0x42, 0x45, 0x45, 0x46), 1)
    val record = new Record(Key.fromString("B"), Array(0x45, 0x45, 0x46))

    assert(recordFromString is record)
    assert(recordFromBytes is record)
  }

  they should "be constructable from stream of byte" in {
    val records =
      Record.fromBytesToRecords(
        LazyList[Byte](0, 1, 2, 3, 4, 5, 6, 7),
        keyLength = 1,
        valueLength = 3
      )

    assert(records.head is Record.fromBytes(Array(0, 1, 2, 3), keyLength = 1))
    assert(records(1) is Record.fromBytes(Array(4, 5, 6, 7), keyLength = 1))
  }
}
