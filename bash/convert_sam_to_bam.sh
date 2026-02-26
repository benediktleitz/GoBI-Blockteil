#!/bin/bash
# convert_sam_to_bam.sh
# Usage: ./convert_sam_to_bam.sh /path/to/sam_folder

set -euo pipefail

SAM_FOLDER="$1"

if [ -z "$SAM_FOLDER" ] || [ ! -d "$SAM_FOLDER" ]; then
    echo "Error: Provide a valid folder containing SAM files."
    exit 1
fi

# Loop over all SAM files in the folder
find "$SAM_FOLDER" -maxdepth 1 -type f -name "*.sam" | while read -r SAM_FILE; do
    # Get mapping algorithm or base name
    MAPPING_ALGO=$(basename "$SAM_FOLDER")       # or extract from filename if needed
    OUTPUT_DIR="output/bams/$MAPPING_ALGO"
    mkdir -p "$OUTPUT_DIR"

    BASENAME=$(basename "$SAM_FILE" .sam)
    BAM_FILE="$OUTPUT_DIR/${BASENAME}.sorted.bam"

    echo "Processing $SAM_FILE → $BAM_FILE"

    # Convert SAM → BAM → sort → index
    samtools view -bS "$SAM_FILE" | samtools sort -o "$BAM_FILE"
    samtools index "$BAM_FILE"

    echo "Done: $BAM_FILE and index created."
done