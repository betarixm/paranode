# Master Proposal

## Overview

Master is responsible for orchestrating the sorting process across multiple worker nodes. Master Node...

1. performs key range estimation.
1. distributes the ranges to the workers.
1. waits for the completion signals from the workers.

## Class: MasterNode

### Member: MasterInfo

- Type: MasterMetadata
- Contains master's IP address and port number.

### Member: WorkersInfo

- Type: `List[WorkerMetadata]`
- Contains all the worker's IP address and port number.
- It doesn't contain key range information.
- Its length is same with number of worker nodes.

### Method: requsetSamples

- Type: `(workersInfo: List[WorkerMetadata]): List[Key]`
- Get sample of keys from all worker node.
- For each worker, get sample of keys as `List[Key]` and flatten the result.
- Master waits for the samples for each worker.

### Method: sortKeys

- Type: `(unsortedKeys: List[Key]): List[Key]`
- Sort `unsortedKeys` in ascending order.

### Method: estimateKeyRanges

- Type: `(sortedKeys: List[Key]): List[KeyRange]`
- Split `sortedKeys` so that we can get `WorkerAddressList.len()` of range.

### Method: makeWorkerList

- Type: `(workersInfo: List[WorkerMetadata], estimatedKeyRangeList: List[KeyRange]): List[WorkerMetadata]`
- Augment `workersInfo` using `estimatedKeyRangeList` so that the output's entry has binding for each worker with one key range.

### Method: broadcastKeyRange

- Type: `(workersInfo: List[WorkerMetadata]):Unit`
- Signal the information of estimated key range to all workers.
- Master waits for the acceptance(?) signal for each worker.

### Method: makeRecordsToPlaceItsCorrectWorker

- Type: `(workersInfo: List[WorkerMetadata]):Unit`
- Signal all workers to have only its own key ranges.
- Workers exchange `block`s each other.
  - Exchanging `block` is consist of sorted `Records` and its total size will be very small size.
- Master waits for the done(?) signal for each worker.

### Method: signalAllWorkersToMergeAndWaitUntilAllWorkerFinishSorting

- Type: `(workerList: List[WorkerMetadata]):Unit`
- Signal all workers to merge its containing blocks.
  - We are using merge sort and each worker previously sort its block.
  - So each sufficient to merge blocks in each worker.
- Master waits for the done(?) signal for each worker.

## Object: Master

### Method: main

- Type: ():Unit
- Use all the public method in class `MasterNode` to make all things work.

## Some Assumption

- Estimated key ranges are always fine in terms of distributing `Ranges`.
  - We could add algorithm to make proper key ranges if needed.
- Exception 'Out of disk space' will not be occured.
  - This is because we assume estimated key ranges are always fine and we exchange `block` which is very small size.
