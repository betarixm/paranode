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

## Object: Exchange RPC Server

### Method: Save Block

- Type: `(block: Block): Unit`
- Saves the given block sent over the RPC connection.

## Object: Worker RPC Server

### Handler: Healthcheck

- Type: `(): Response[Boolean]`
- Returns any message if a worker is ready.

### Handler: Sample

- Type: `(): Response[List[Key]]`
- Return sampled keys of a worker.

### Handler: Sort

- Type: `(): Response[Boolean]`
- Sort a worker's blocks.

### Handler: Partition

- Type: `(workers: List[WorkerMetadata]): Response[Boolean]`
- Make partitions based on given workers' metadata.

### Handler: Exchange

- Type: `(workers: List[WorkerMetadata]): Response[Boolean]`
- Send partitions to dedicated workers.

### Handler: Finalize

- Type: `(): Response[Boolean]`
- Merge saved partitions and write into files.

### Method: Main

- Type: `(): Unit`

1. Init blocks from given directories.
1. Spawn exchange RPC server.
1. Spawn worker RPC server.
