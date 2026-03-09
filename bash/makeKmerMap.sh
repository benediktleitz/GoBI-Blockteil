#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAR="${JAR:-gta_filter.jar}"
FW="${FW:-data/pig-data-rnaseq/H5-12939-T2_R1_001.fastq.gz}"
RW="${RW:-data/pig-data-rnaseq/H5-12939-T2_R3_001.fastq.gz}"
FASTA="${FASTA:-data/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz}"
GTF="${GTF:-data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz}"
GENES="${GENES:-output/plotting_data/quality/dna/gene_list.txt}"
OUT_DIR="${OUT_DIR:-KMER}"
THRESHOLD="${THRESHOLD:-105}"

declare -a K_VALUES=()
for k in $(seq 12 1 30); do
  K_VALUES+=("$k")
done


mkdir -p "$OUT_DIR"

echo "Creating k-mer maps in $OUT_DIR"
for k in "${K_VALUES[@]}"; do
  out_file="$OUT_DIR/k_${k}.txt"
  echo "k=$k -> $out_file"
  java -jar "$JAR" \
    -fw "$FW" \
    -rw "$RW" \
    -k "$k" \
    -offset "$k" \
    -threshold "$THRESHOLD" \
    -fasta "$FASTA" \
    -od "$OUT_DIR" \
    -gtf "$GTF" \
    -genes "$GENES" \
    -kmerMap "$out_file"
done

echo "Done."
