#!/bin/bash

if [ $# -lt 1 ]; then
    echo "Usage: $0 <DIRECTORY>"
    exit 1
fi

DIRECTORY=$1

total=0

if [ -d "$DIRECTORY" ]; then
    files="$DIRECTORY/*"
    for file in $files; do
        if [ -f "$file" ]; then
            lines=$(wc -l <"$file")
            total=$((total + lines))

            echo "File: $file - Lines: $lines"
        fi
    done

    # Output the total line count
    echo "Total lines in all files: $total"
else
    echo "Directory does not exist."
fi
