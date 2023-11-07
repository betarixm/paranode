package kr.ac.postech.paranode.core

import org.scalatest.flatspec.AnyFlatSpec

class BlockSpec extends AnyFlatSpec {
  "Block" should "be constructable from bytes" in {
    val bytes = LazyList[Byte](0, 1, 2, 3, 4, 5, 6, 7)
    val block = Block.fromBytes(bytes, keyLength = 1, valueLength = 3)

    val expectedBlock =
      new Block(
        LazyList[Record](
          new Record(new Key(Array(0)), Array(1, 2, 3)),
          new Record(new Key(Array(4)), Array(5, 6, 7))
        )
      )

    val firstRecord = block.records.head
    val secondRecord = block.records(1)
    val expectedFirstRecord = expectedBlock.records.head
    val expectedSecondRecord = expectedBlock.records(1)

    assert(firstRecord is expectedFirstRecord)
    assert(secondRecord is expectedSecondRecord)
  }
}
