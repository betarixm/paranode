package kr.ac.postech.paranode.core

import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import scala.io.Source

class BlockSpec extends AnyFlatSpec {
  implicit class ComparableKeyRange(keyRange: KeyRange) {
    def is(that: KeyRange): Boolean =
      (keyRange.from is that.from) && (keyRange.to is that.to)
  }

  implicit class ComparablePartition(partition: Partition) {
    def is(that: Partition): Boolean = {
      (partition._1 is that._1) && (partition._2 is that._2)
    }
  }

  implicit class ComparableBlock(block: Block) {
    def is(that: Block): Boolean = {
      block.records
        .zip(that.records)
        .forall(records => records._1 is records._2)
    }
  }

  implicit class ComparableRecord(sampledKeyList: LazyList[Key]) {
    def is(that: LazyList[Key]): Boolean = {
      sampledKeyList
        .zip(that)
        .forall(keys => keys._1 is keys._2)
    }
  }

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

    assert(block is expectedBlock)
  }

  it should "be constructable from source" in {
    val source = Source.fromBytes(Array(0, 1, 2, 3, 4, 5, 6, 7))
    val block =
      Block.fromSource(source, keyLength = 1, valueLength = 3)

    val expectedBlock =
      new Block(
        LazyList[Record](
          new Record(new Key(Array(0)), Array(1, 2, 3)),
          new Record(new Key(Array(4)), Array(5, 6, 7))
        )
      )

    assert(block is expectedBlock)
  }

  it should "be constructable from a path" in {
    val path = getClass.getResource("/block-spec-8.raw").getPath

    val block =
      Block.fromPath(path, keyLength = 1, valueLength = 3)

    val expectedBlock =
      new Block(
        LazyList[Record](
          new Record(new Key(Array(0x30)), Array(0x31, 0x32, 0x33)),
          new Record(new Key(Array(0x34)), Array(0x35, 0x36, 0x37))
        )
      )

    assert(block is expectedBlock)
  }

  it should "be serializable as chars" in {
    val block = new Block(
      LazyList(
        new Record(new Key(Array(0x0)), Array(0x1, 0x2, 0x3)),
        new Record(new Key(Array(0x4)), Array(0x5, 0x6, 0x7))
      )
    )

    assert(
      block.toChars sameElements Array[Char](0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6,
        0x7)
    )
  }

  it should "be writable to a file" in {
    val block = new Block(
      LazyList(
        new Record(new Key(Array(0x0)), Array(0x1, 0x2, 0x3)),
        new Record(new Key(Array(0x4)), Array(0x5, 0x6, 0x7))
      )
    )

    val temporaryFile = File.createTempFile("block-spec-temp", ".raw")

    val result = block.writeTo(temporaryFile.getPath)

    val source = Source.fromFile(result)

    temporaryFile.deleteOnExit()

    assert(
      source.toArray sameElements Array[Char](0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6,
        0x7)
    )
  }

  it should "be able to make partition" in {
    val firstKeyRange = KeyRange(new Key(Array(0x0)), new Key(Array(0x4)))
    val secondKeyRange = KeyRange(new Key(Array(0x5)), new Key(Array(0x9)))

    val blocks = new Block(
      LazyList(
        new Record(new Key(Array(0x1)), Array(0x1, 0x2, 0x3)),
        new Record(new Key(Array(0x6)), Array(0x5, 0x6, 0x7)),
        new Record(new Key(Array(0x2)), Array(0x8, 0x9, 0x7))
      )
    )

    val partitions = blocks.partition(List(firstKeyRange, secondKeyRange))

    val expectedPartitions = List(
      new Partition(
        firstKeyRange,
        new Block(
          LazyList(
            new Record(new Key(Array(0x1)), Array(0x1, 0x2, 0x3)),
            new Record(new Key(Array(0x2)), Array(0x8, 0x9, 0x7))
          )
        )
      ),
      new Partition(
        secondKeyRange,
        new Block(
          LazyList(
            new Record(new Key(Array(0x6)), Array(0x5, 0x6, 0x7))
          )
        )
      )
    )

    assert(
      partitions
        .zip(expectedPartitions)
        .forall(partitions => partitions._1 is partitions._2)
    )
  }

  it should "be sortable" in {
    val block = new Block(
      LazyList(
        new Record(new Key(Array(0x4)), Array(0x5, 0x6, 0x7)),
        new Record(new Key(Array(0x0)), Array(0x1, 0x2, 0x3)),
        new Record(new Key(Array(0xc)), Array(0xd, 0xe, 0xf)),
        new Record(new Key(Array(0x8)), Array(0x9, 0xa, 0xb))
      )
    )

    val sortedBlock = block.sorted

    val expectedBlock =
      new Block(
        LazyList(
          new Record(new Key(Array(0x0)), Array(0x1, 0x2, 0x3)),
          new Record(new Key(Array(0x4)), Array(0x5, 0x6, 0x7)),
          new Record(new Key(Array(0x8)), Array(0x9, 0xa, 0xb)),
          new Record(new Key(Array(0xc)), Array(0xd, 0xe, 0xf))
        )
      )

    assert(sortedBlock is expectedBlock)
  }

  it should "be able to be sampled" in {
    val block =
      new Block(
        LazyList(
          new Record(new Key(Array(0x0)), Array(0x1, 0x2, 0x3)),
          new Record(new Key(Array(0x4)), Array(0x5, 0x6, 0x7)),
          new Record(new Key(Array(0x8)), Array(0x9, 0xa, 0xb)),
          new Record(new Key(Array(0xc)), Array(0xd, 0xe, 0xf))
        )
      )

    val sample = block.sample()

    val key1 = new Key(Array(0x0))
    val key2 = new Key(Array(0x4))
    val key3 = new Key(Array(0x8))
    val key4 = new Key(Array(0xc))

    val expectedSample = LazyList(key1, key2, key3, key4)

    assert(sample is expectedSample)
  }
}
