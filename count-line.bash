#!/bin/bash

# Directory containing the files
DIRECTORY="/home/green/434project/output/"

# Variable to store the total line count
total_lines=0

# Check if the directory exists
if [ -d "$DIRECTORY" ]; then
    # Loop through each file in the directory
    for FILE in "$DIRECTORY"*
    do
        # Check if it's a regular file
        if [ -f "$FILE" ]; then
            # Count the lines in the file
            LINE_COUNT=$(wc -l < "$FILE")

            # Add the line count to the total
            total_lines=$((total_lines + LINE_COUNT))

            # Output the line count for this file
            echo "File: $FILE - Lines: $LINE_COUNT"
        fi
    done

    # Output the total line count
    echo "Total lines in all files: $total_lines"
else
    echo "Directory does not exist."
fi
