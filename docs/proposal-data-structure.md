# Data Structure Proposal

## Data: Key

- Extends `AnyVal` with `Ordered[Key]`

### Member: raw

- Type: `List[Byte]`
- Original data for key.

### Method: compare

- Type: `(that: Key): Int`
- Used for sorting and comparing between keys.

## Data: Record

- Extend `Ordered[Record]`

### Member: Key

- Type: `Key`

### Member: Value

- Type: `List[Byte]`

### Constructor: From Bytes

- Type: `(bytes: List[Byte]): Record`
- Create `Record` from list of bytes.

### Method: Compare

- Type: `(that: Record): Int`
- Compare between records based on keys.

### Method: Serialize

- Type: `(): List[Byte]`
- Make key and value into bytes.

## Data: Block

### Member: Records

- Type: `Stream[Record]`

### Constructor: From File

- Type: `(file: ?): Block`
- Create block from file.

### Method: Serialize

- Type: `(): Stream[Byte]`
- Make records into bytes.

### Method: Sort

- Type: `(): Block`
- Return new sorted block.

### Method: Partition

- Type: `(metadata: List[WorkerMetadata]): List[Partition]`
- Make partitions based on workers' metadata.

### Method: Sample

- Type: `(): List[Key]`
- Sample keys from block.

## Data: KeyRange

- Type: `Tuple2[Key, Key]`

## Data: Partition

- Type: `Tuple2[WorkerMetadata, Block]`

## Data: MasterMetadata

### Member: Host

- Type: `String`

### Member: Port

- Type: `Int`

## Data: WorkerMetadata

### Member: Key Range

- Type: `KeyRange`

### Member: Host

- Type: `String`

### Member: Port

- Type: `Int`

## Data: Partition

- Type: `Tuple2[WorkerMetadata, Block]`
