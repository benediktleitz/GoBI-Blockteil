#!/usr/bin/env bash
OUTPUT_FILE="output/read_counts.txt"
echo -e "filename\tread_count" > "$OUTPUT_FILE"
for file in data/pig-data-rnaseq/*; do
    echo "doing file ${file:21:18}"
    if [ -f "$file" ]; then
        echo -ne "${file:21:18}\t" >> "$OUTPUT_FILE"
        zcat "$file" | wc -l | xargs -n 1 bash -c 'echo $(($0 / 4))' >> "$OUTPUT_FILE"
    fi
done