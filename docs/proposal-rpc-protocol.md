# RPC Protocol Proposal

## Background

gRPC is a modern open-source framework for high-performance Remote Procedure Call (RPC) operations. It is built on the HTTP/2 protocol, allowing it to support both traditional request/response interactions and streaming requests and responses.

- Definition of service methods.
- Transmission of messages over the network.
- Enabling client applications to call methods on server applications located on different machines as though they were local objects.

For the translation of Protocol Buffers into Scala, the project will utilize [ScalaPB](https://scalapb.github.io/). The `build.sbt` file will include the necessary gRPC dependencies.

## Services

Services and their method specifications are defined in Protocol Buffers (`proto` files). Upon compilation, the `proto` file generates code for both the server and client sides.

### Worker Exchange RPC Service

#### RPC: Save Block

- Type: `(SaveBlockRequest): SaveBlockResponse`

### Worker RPC Service

#### RPC: Sample

- Type: `(SampleRequest): SampleResponse`

#### RPC: Sort

- Type: `(SortRequest): SortResponse`

#### RPC: Partition

- Type: `(PartitionRequest): PartitionResponse`

#### RPC: Exchange

- Type: `(ExchangeRequest): ExchangeResponse`

#### RPC: Finalize

- Type: `(FinalizeRequest): FinalizeResponse`

### Master RPC Service

#### RPC: Register

- Type: `(RegisterRequest): RegisterResponse`

### Messages

Each RPC's request and response messages' structure can be found at worker and master proposals as type of handler's parameter.

## Flow

To accomplish distributed and parallel sorting of key-value records stored across multiple disks on multiple machines, the design of a distributed system architecture entails the following steps, utilizing gRPC as the communication layer between the various components of the system.

### Step 1: Opening an RPC Server between master and workers

The master opens the server and awaits calls while the worker creates a client that connects to master. The worker then calls the RPC method using a "RPC: Register".

#### Detailed Sequence

- The master opens a server at startup and waits for calls from the workers.
- Workers begin by loading the blocks concurrently.
- Upon successful block loading, workers send a message to the master.

### Step 2: Sampling Records and Sending Keys to the Master

This step can occur concurrently with the previous step. The master does not wait for all workers to successfully load the block but signals workers who finish loading early to start sampling.

#### Detailed Sequence

- Workers start sampling upon receiving a command from the master.
- They send their sampled data to the master as a message.
- The master waits until all workers have sent their sampled keys, then generates worker metadata and sends it to each worker.

### Step 3: Block Partitioning

Once all workers have sampled keys from records and sent them to the master, the master sends metadata so workers can divide their block into partitions. This step does not require RPC or message exchanges.

#### Detailed Sequence

- Workers receive metadata from the master.
- Workers then divide the sorted data into partitions.

### Step 4: Exchanging Partitions Between Workers

After partitioning, each worker opens their "Exchange RPC Service" to wait for incoming partitions and sends out their partitions to the corresponding workers.

### Step 5: Merging All Partitions into a Single Block

Once the partition exchange is complete, the master request the merging of all partitions into a single block.
