# Worker Proposal

## Class: Blocks

### Member: Source

- Type: `List[Block]`

### Method: Sample

- Type: `(): List[Key]`
- Sample keys from the source using the `Block.sample` method.

### Method: Sorted

- Type: `(): Blocks`
- Create a new `Blocks` instance where each block is sorted by `Block.sort`. Note that this step should be executed in parallel.

### Method: Partition

- Type: `(): (workers: List[WorkerMetadata]): List[Partition]`
- Create a list of partitions based on key ranges of workers.

### Method: Merge

- Type: `(): Stream[Block]`
- Create a stream of blocks. Ensure that the stream is in a sorted state.

### Method: Serialize

- Type: `(): Stream[Byte]`
- Convert the blocks into a stream of bytes.

### Method: Save

- Type: `(): (directory: Directory): Directory`
- Write the serialized data into the given directory and return the updated directory.

## Object: RPC Server

### Method: Save Block

- Type: `(block: Block): Unit`
- Saves the given block sent over the RPC connection.

## Object: Worker

### Method: Main

- Type: `(): Unit`

1. Create blocks based on the given input directories.
1. Make a sorted version of the blocks using the `Blocks.sorted` method.
1. Partition the sorted blocks and obtain a list of worker metadata.
1. Signal the master node that the worker is ready.
1. Wait for a signal that enables data exchange, including workers' metadata.
1. Spawn an RPC server for receiving partitions from other workers.
1. Send blocks to other workers, iterating over partitions.
1. Reconstruct blocks with saved partitions.
1. Merge partitions and save them into files.
1. Signal the master node that the worker is done.
