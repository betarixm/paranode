# Master Proposal

## Overview

Master is responsible for orchestrating the sorting process across multiple worker nodes. Master node:

1. Performs key range estimation.
1. Distributes the ranges to the workers.
1. Waits for the completion signals from the workers.

## Object: Master RPC Server

### Handler: Register

- Type: `(worker: WorkerMetadata): Response[Boolean]`
- Register given worker.

## Class: Master

### Member: Master Info

- Type: `MasterMetadata`
- Contains master's IP address and port number.

### Member: Workers

- Type: `List[WorkerMetadata]`
- Contains all the worker's IP address and port number.
- It doesn't contain key range information.
- Its length is same with number of worker nodes.

### Method: Wait for Workers

- Type: `(): List[WorkerMetadata]`
- Wait for given number of workers to be registered.

### Method: Sample

- Type: `(workersInfo: List[WorkerMetadata]): List[Key]`
- Get samples of keys from all worker node.
- For each worker, get sample of keys as `List[Key]` and flatten the result.
- Master waits for the samples for each worker.

### Method: Sort Keys

- Type: `(unsortedKeys: List[Key]): List[Key]`
- Sort `unsortedKeys` in ascending order.

### Method: Estimate Key Ranges

- Type: `(sortedKeys: List[Key]): List[KeyRange]`
- Split `sortedKeys` so that we can get `WorkerAddressList.len()` of range.

### Method: Create Worker List From Key Ranges

- Type: `(workersInfo: List[WorkerMetadata], estimatedKeyRangeList: List[KeyRange]): List[WorkerMetadata]`
- Augment `workersInfo` using `estimatedKeyRangeList` so that the output's entry has binding for each worker with one key range.

### Method: Broadcast Key Ranges

- Type: `(workersInfo: List[WorkerMetadata]): Unit`
- Signal estimated key ranges to all workers.
- Signaled workers should make partitions.
- Master waits for the response for each worker.

### Method: Exchange

- Type: `(workersInfo: List[WorkerMetadata]): Unit`
- Signal all workers to have only its own key ranges.
- Workers exchange `Block`s each other.
  - Exchanging `Block` is consist of sorted `Record`s and its total size will be affordable size.
- Master waits for the response for each worker.

### Method: Finalize

- Type: `(workerList: List[WorkerMetadata]): Unit`
- Signal all workers to merge its containing blocks.
  - We are using merge sort and each worker previously sort its block.
  - So each sufficient to merge blocks in each worker.
- Master waits for the response for each worker.

### Method: Main

- Type: `(): Unit`

1. Wait for workers to be registered.
1. Sample keys from workers.
1. Estimate key ranges based on sorted sampled keys.
1. Broadcast key ranges to workers and request workers to make partitions.
1. Request workers to exchange
1. Request workers to finalize

## Some Assumption

- Estimated key ranges are always fine in terms of distributing `Record`s.
  - We could add algorithm to make proper key ranges if needed.
- Exception 'Out of disk space' will not be occurred.
  - This is because we assume estimated key ranges are always fine and we exchange `Block` which is affordable size.
