package kr.ac.postech.paranode.core

import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import scala.io.Source

class BlockSpec extends AnyFlatSpec {
  implicit class ComparableBlock(block: Block) {
    def is(that: Block): Boolean = {
      block.records
        .zip(that.records)
        .forall(records => records._1 is records._2)
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
    val keyStart_1 = new Key(Array(0x0))
    val keyEnd_1 = new Key(Array(0x4))
    val keyRange_1 = new KeyRange(keyStart_1, keyEnd_1)
    val workerMetadata_1 = WorkerMetadata("1.1.1.1", 123, Option(keyRange_1))

    val keyStart_2 = new Key(Array(0x5))
    val keyEnd_2 = new Key(Array(0x9))
    val keyRange_2 = new KeyRange(keyStart_2, keyEnd_2)
    val workerMetadata_2 = WorkerMetadata("2.2.2.2", 123, Option(keyRange_2))

    val workers = List(workerMetadata_1, workerMetadata_2)

    val block1 = new Block(
      LazyList(
        new Record(new Key(Array(0x1)), Array(0x1, 0x2, 0x3)),
        new Record(new Key(Array(0x2)), Array(0x8, 0x9, 0x7))
      )
    )
    val block2 = new Block(
      LazyList(
        new Record(new Key(Array(0x6)), Array(0x5, 0x6, 0x7))
      )
    )
    val blocks = new Block(
      LazyList(
        new Record(new Key(Array(0x1)), Array(0x1, 0x2, 0x3)),
        new Record(new Key(Array(0x6)), Array(0x5, 0x6, 0x7)),
        new Record(new Key(Array(0x2)), Array(0x8, 0x9, 0x7))
      )
    )
    val result = blocks.partition(workers)

    val answer = List(
      new Partition(workerMetadata_1, block1),
      new Partition(workerMetadata_2, block2)
    )

  }

}
