#!/usr/bin/env bash
set -euo pipefail

gene_id="ENSSSCG00000011316"
bam_file="data/pig-data-rnaseq/mapped/minimap2/H5-12939-T2.sorted.bam"
gtf_file="data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz"
ref_fasta="data/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz"
missing_reads="testFiles/${gene_id}_missingReads.txt"


if [[ "$gtf_file" == *.gz ]]; then
	gene_line=$(zcat -- "$gtf_file" | awk -F '\t' -v gid="$gene_id" '$3 == "gene" && $9 ~ ("gene_id \"" gid "\"") && !found { print; found=1 }')
else
	gene_line=$(awk -F '\t' -v gid="$gene_id" '$3 == "gene" && $9 ~ ("gene_id \"" gid "\"") && !found { print; found=1 }' "$gtf_file")
fi

if [[ -z "$gene_line" ]]; then
	echo "Error: gene_id '$gene_id' not found as feature 'gene' in $gtf_file" >&2
	exit 1
fi

chr=$(awk -F '\t' '{print $1}' <<< "$gene_line")
start=$(awk -F '\t' '{print $4}' <<< "$gene_line")
end=$(awk -F '\t' '{print $5}' <<< "$gene_line")
region="${chr}:${start}-${end}"

output_reads="testFiles/${gene_id}_reads.txt"
output_fasta="testFiles/${chr}_chromosome.fa"
mkdir -p "$(dirname "$output_reads")" "$(dirname "$output_fasta")"


echo "Gene region: $region"

samtools view -F 0x900 "$bam_file" "$region" \
	| cut -f1 \
	| sort -u \
	> "$output_reads"

echo "Wrote read IDs to: $output_reads"
echo "Wrote chromosome FASTA to: $output_fasta"

python3 python/test.py

samtools view -b -N "$missing_reads" "$bam_file" > "${missing_reads%.txt}.bam"
samtools index "${missing_reads%.txt}.bam"

samtools faidx "$ref_fasta" "$chr" > "$output_fasta"



