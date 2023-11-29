#!/bin/bash

# Number of data directories (M) and files in each directory (N)
numberOfDirectories=2
numberOfFiles=2
numberOfRecords=4000

# Base directory
baseDir="/home/green/434project"

# Step 2: Remove files in each data directory and in the output directory
for (( dir=0; dir<numberOfDirectories; dir++ )); do
    rm -rf "${baseDir}/data${dir}"/*
done

# Removing files in the output directory
rm -rf "${baseDir}/output"/*

# Step 3: Run the command for each file in each data directory
for (( dir=0; dir<numberOfDirectories; dir++ )); do
    for (( file=0; file<numberOfFiles; file++ )); do
        ../64/gensort -a -s $numberOfRecords "${baseDir}/data${dir}/${file}"
    done
done
