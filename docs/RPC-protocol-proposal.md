# Background

gRPC is a modern open-source framework for high-performance Remote Procedure Call (RPC) operations. It is built on the HTTP/2 protocol, allowing it to support both traditional request/response interactions and streaming requests and responses.

- Definition of service methods.
- Transmission of messages over the network.
- Enabling client applications to call methods on server applications located on different machines as though they were local objects.

For the translation of Protocol Buffers into Scala, the project will utilize [ScalaPB](https://scalapb.github.io/). The `build.sbt` file will include the necessary gRPC dependencies.

# Design of a Distributed System Architecture with gRPC

To accomplish distributed/parallel sorting of key/value records stored across multiple disks on multiple machines, the design of a distributed system architecture entails the following steps, utilizing gRPC as the communication layer between the various components of the system:

## Server-Client Structure

_TODO: Insert architecture diagram_

## Message Definition

- `successLoadBlock`
- `startKeySampling`
- `SampledKey`
- `workerMetadata`
- `notMinePartition`

## Service Definition

Services and their method specifications are defined in Protocol Buffers (proto files). Upon compilation, the proto file generates code for both the server and client sides. Typically, the master performs the role of the server while workers act as clients, but sometimes a worker must also initiate a server to facilitate communication between workers.

### Step 1: Opening an RPC Server between Master and Worker

The master opens the server and awaits calls while the worker creates a client that connects to this server. The worker then calls the RPC method using a stub.

- **Master’s RPC Methods:**
    - `isFinishWorkerLoadBlock()`: Waits until all workers have sent their sampled keys.
    - Type: `(successLoadBlock) => startKeySampling`

- **Worker’s RPC Methods:**
    - `finishLoadBlock()`: Notifies the master when the block load is complete and receives a command to start sampling.

#### Detailed Sequence

- The master opens a server at startup and waits for calls from the workers.
- Workers begin by loading the blocks concurrently.
- Upon successful block loading, workers send a message to the master and receive a command to start sampling.

### Step 2: Sampling Records and Sending Keys to the Master

This step can occur concurrently with the previous step. The master does not wait for all workers to successfully load the block but signals workers who finish loading early to start sampling.

- **Master’s RPC Methods:**
    - `isFinishAllWorkerSamplingData()`: Waits until all workers have sent their sampled keys.
    - Type: `SampledKey => workerMetadata`

- **Worker’s RPC Methods:**
    - `finishKeySampling()`: Notifies the master when key sampling is complete and receives metadata in return.

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

After partitioning, each worker opens their server to wait for incoming partitions and sends out their partitions to the corresponding workers. The master monitors the exchange using a wait RPC.

- **Master’s RPC Methods:**
    - `isFinishAllWorkerExchangePartition()`: Waits until all workers have finished exchanging partitions.

- **Worker’s RPC Methods:**
    - `sendPartitionToOwner()`: Sends partitions to the corresponding worker and confirms receipt.
    - Type: `notMinePartition => Bool`

### Step 5: Checking the Completion of Partition Exchange

_TODO: Define detailed algorithm and corresponding RPC methods and message types._

### Step 6: Merging All Partitions into a Single Block

Once the partition exchange is complete, the master initiates the merging of all partitions into a single block.

- **Master’s RPC Methods:**
    - `mergeAllPartitionToBlock()`: Starts the merge process.
    - Type: `Unit => Bool`
