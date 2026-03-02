#!/bin/bash
ref_fasta="data/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz"
out_dir="testFiles/igv/fasta"

mkdir -p "$out_dir"

for chr in {1..18}; do
    samtools faidx "$ref_fasta" "$chr" > "${out_dir}/${chr}.fa"
done