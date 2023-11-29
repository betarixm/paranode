#!/bin/bash

# Check if sufficient arguments are provided
if [ $# -lt 3 ]; then
    echo "Usage: $0 <numberOfDirectories> <numberOfFiles> <numberOfRecords>"
    exit 1
fi

# Assign the arguments to variables
numberOfDirectories=$1
numberOfFiles=$2
numberOfRecords=$3

# Base directory
baseDir="/home/green/434project"

# Step 2: Check and create directories if needed, then remove files in each data directory and in the output directory
for (( dir=0; dir<numberOfDirectories; dir++ )); do
    dataDir="${baseDir}/data${dir}"
    # Check if directory exists, if not, create it
    mkdir -p "$dataDir"
    # Remove files in the directory
    rm -rf "${dataDir}"/*
done

# Checking and creating the output directory
outputDir="${baseDir}/output"
mkdir -p "$outputDir"
# Removing files in the output directory
rm -rf "${outputDir}"/*

# Step 3: Run the command for each file in each data directory
for (( dir=0; dir<numberOfDirectories; dir++ )); do
    for (( file=0; file<numberOfFiles; file++ )); do
        ../64/gensort -a -s $numberOfRecords "${baseDir}/data${dir}/${file}"
    done
done
