# Architecture Proposal

## Problem Description

### Goal

- Implement system that can sort key/value records stored across **multiple disks**, using **multiple machines** with **distributed/parallel** programming

### Condition

- Data
  - get data from <http://www.ordinal.com/gensort.html>
    - one record is consist of 10 bytes of key and 90 bytes of value
    - sort data using key with lexicographical order
- Resources
  - one Master Node
  - fixed number of Worker Nodes
- Input & Output
  - Input: bunch of 32MB files which is allocated in each Worker
  - Output: ordering of ???MB files with sorted data, in each Worker

## Method

Basically we are going to use merge sort. To fully exploit N number of Workers, we have to sort parallel in each Worker. So hole procedure could be divided into two phase as below.

1. Relocate data in each Worker.
2. Each Worker sort its relocated data in each Worker's disk.

Now it's time to deep dive into each two phase with specific step.

### First Phase

To exploit all the Workers in parallel, we have to distribute the recordes into each Worker.

#### Step1: Data Sampling

- Sample a small subset of records from each Worker Node.
- Send key of these samples to the Master Node.

#### Step2: Key Range Estimation

- Master Node sorts all the received keys.
- Master Node then divied sorted keys into 'n' ranges, where 'n' is the number of Worker Nodes.

#### Step3: Broadcast Key Ranges

- Master Node broadcasts the estimated key ranges to all Worker Nodes.

#### Step4: Make Group(Partition)

- Each Worker Node then divide its local data according to the received key ranges.
  - Any key that falls within a paricular range will be grouped together.
- Detailed explaination for using multiple cores is in "Worker Proposal" document.

#### Step5: Relocating

- Master Node pick two Worker Nodes and change its block until there is nothing to relocate.
  - block: the unit of moving data.
- Detailed explaination is in "Master Proposal" document.
- Example for brief understaning of Step5.
  - Suppose there are three Worker Nodes named A, B, and C.
    - Notation
      - Worker name's meta variable is X, Y.
      - R[X]: Range of each Worker Node X.
      - D[X,Y]: Rough sorted data of range R[Y] in X.
  - 1. Master Node pick A and B.
  - 2. Then A gives one block in D[A,B] to Master Node.
  - 3. Then B gives one block in D[B,A] to Master Node.
  - 4. Do this until there is no block to relocate for both A and B.
  - 5. Master Node pick A and C, then do 2~4.
  - 6. Master Node pick B and C, then do 2~4.

### Second Phase

After data relocation, each Worker Node has its local data organized into partitions. In the second phase, all we have to do is just sorting the data within each partition. This automatically lead to overall sorting.

#### Step1: Local Parallel Sorting

- Each Worker does merge sort in parallel with respect to other Worker.
- Detailed explaination for using multiple cores is in "Worker Proposal" document.

#### Step2: Singal Sorting Completion to Master

- Upon completing the sort for all partitions, each Worker Node sends a signal to the Master Node indicating that it has finished sorting.
- The Master Node waits until all Worker Nodes gives signal to it.
- If all Worker Node gives signal to Master Node, program ends.

## More Detail

Until now, we got a big picture of achieving Goal, mainly about answering the question of "How to corrodinate Multiple Nodes?". The four Proposal document below will give you detailed explaination for each subject.

### Data Structure Proposal

- Define fundamental data structure that we will use through this project.

### RPC Protocol Proposal

- Data Sample Request and Reply
  - Master sends a request to each Worker.
  - Each Worker replies with a set of sample keys.
- Key Range Broadcast
  - Master broadcasts the key ranges to all Worker Nodes.
- Relocation Request and Reply
  - Master sends a request for data block movement.
  - Worker sends a reply the data block that will relocated.
- Sorting Complete Signal
  - Worker sends a signal to the Master.
  - Master keeps a count and waits for signals from all Workers.

### Worker Proposal

- Data Sampling
  - On request from the Master Node, the Worker sends back a small subset of records.
- Key Range Partitioning
  - Upon receiving key ranges, partition data based on those ranges.
  - Use parallelism
- Local Sorting
  - Perform merge sort on each partition.
  - Use parallelism
- Signalling
  - Once sorting is done, send a signal to the Master Node.

### Master Proposal

- Data Sampling
  - Request a set of sample records from each Worker Node.
- Key Range Estimation
  - Sort the keys and divide them into ranges.
- Key Range Broadcast
  - Broadcast the key ranges to all Worker Nodes.
- Block Relocation
  - Continuously pick two Workers and oversee the exchange of data blocks until there is nothing left to relocate.
- Waiting Mechanism
  - Wait for the signal from all Worker Nodes that they have completed sorting.
