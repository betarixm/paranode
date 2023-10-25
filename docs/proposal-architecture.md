# Architecture Proposal Report

## Problem Setting

- Data
    - get data from http://www.ordinal.com/gensort.html
        - one record is consist of 10 bytes of key and 90 bytes of value 
        - sort data using key with lexicographical order
- Resources
    - one Master Node
    - fixed number of Worker Nodes 
- Input & Output
    - Input: bunch of 32MB files which is allocated in each Worker
    - Output: ordering of ???MB files with sorted data, in each Worker 

## Goal
- Implement system that can sort key/value records stored across **multiple disks**, using **multiple machines** with **distributed/parallel** programming

## Method

Basically we are going to use merge sort. To fully exploit N number of Workers, we have to sort parallel in each Worker. So hole procedure could be divided into two phase as below.

1. Relocate data in each Worker.
2. Each Worker sort its relocated data in each Worker's disk.

Now it's time to deep dive into each two phase with specific step.

### First Phase 
To exploit all the Workers in parallel, we have to distribute the sort-thing into each Worker.

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
#### Step5: Relocating
- Master Node pick two Worker Nodes and change its block until there is nothing to relocate.
    - block: the unit of moving data.
- Example
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

Now it's time to real sort in each Worker machine.
How to expliot Multiple Cores?


## Implementation of Each Node
### Master Node
### Worker Node



