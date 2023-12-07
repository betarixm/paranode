#!/bin/bash

# Check if sufficient arguments are provided
if [ $# -lt 3 ]; then
    echo "Usage: $0 <BASE_DIRECTORY> <NUMBER_OF_DIRECTORIES> <NUMBER_OF_FILES> <NUMBER_OF_RECORDS>"
    exit 1
fi

SCRIPT_PATH=$(realpath "$0")
SCRIPTS_DIR=$(dirname "$SCRIPT_PATH")
PROJECT_DIR=$(dirname "$SCRIPTS_DIR")

GENSORT="$PROJECT_DIR/bin/gensort"

BASE_DIRECTORY=$1
NUMBER_OF_DIRECTORIES=$2
NUMBER_OF_FILES=$3
NUMBER_OF_RECORDS=$4

for ((i = 0; i < NUMBER_OF_DIRECTORIES; i++)); do
    dir="${BASE_DIRECTORY}/input${i}"
    mkdir -p "$dir"
    rm -rf "${dir}"/*

    for ((j = 0; j < NUMBER_OF_FILES; j++)); do
        $GENSORT -a -s $NUMBER_OF_RECORDS "${dir}/${j}"
    done
done

# Checking and creating the output directory
outputDirectory="${BASE_DIRECTORY}/output"
mkdir -p "$outputDirectory"

# Removing files in the output directory
rm -rf "${outputDirectory}"/*
