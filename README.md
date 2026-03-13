# CIKS7

CIKS7 (Counting and Identification using K-mer based Selection) is a fast k-mer-based pre-filter for paired-end sequencing data. It identifies reads originating from user-defined genes or genomic regions and reduces the number of reads that must be processed by downstream mappers.

CIKS7 supports:

- DNA and RNA filtering modes
- paired-end `AND` (default) and `OR` logic
- optional SNP k-mer augmentation per gene
- configurable multithreaded processing

A detailed methodological description and benchmarking results are provided in the accompanying paper PDF included in this repository. On typical RNA-seq datasets, CIKS7 processes approximately **13–14 million reads per minute** using default settings.

## Requirements

- Java Runtime Environment (JRE) 24 or newer
- paired-end FASTQ input files
- reference FASTA

Optional, depending on run mode:

- GTF annotation file
- gene list file
- SNP k-mer directory

## Installation

CIKS7 is distributed as a runnable JAR.

1. Download `ciks7.jar` from this repository (or from a release asset, if provided).
2. Place it in your working directory.
3. Verify Java availability:

```bash
java -version
```

4. Verify the tool starts:

```bash
java -jar ciks7.jar -h
```

## Quickstart

Minimal DNA run for a single region:

```bash
java -jar ciks7.jar \
	-fw reads_1.fastq.gz \
	-rw reads_2.fastq.gz \
	-k 15 \
	-offset 15 \
	-threshold 90 \
	-fasta reference.fa \
	-chr chr7 \
	-start 5500000 \
	-end 5600000 \
	-od results \
	-counts -tsv
```

RNA run with GTF and gene list:

```bash
java -jar ciks7.jar \
	-fw reads_1.fastq.gz \
	-rw reads_2.fastq.gz \
	-k 15 \
	-offset 15 \
	-threshold 90 \
	-fasta reference.fa \
	-gtf annotation.gtf \
	-genes genes.txt \
	-rna \
	-od results_rna \
	-counts -tsv
```

## Usage

General form:

```bash
java -jar ciks7.jar [options]
```

### Required Parameters

- `-fw` First FASTQ file (paired-end mate 1)
- `-rw` Second FASTQ file (paired-end mate 2)
- `-k` K-mer size
- `-offset` Offset used for read k-mer generation
- `-threshold` Minimum number of matching bases inferred from k-mers required for a read to pass the filter
- `-fasta` Reference genome FASTA
- `-od` Output directory

### Target Definition Modes

Choose one target-definition strategy:

1. Single genomic region using:
- `-chr`
- `-start` (1-based)
- `-end` (1-based)

2. Gene-based selection from GTF using:
- `-gtf`
- optional `-genes` (newline-separated gene IDs)

If `-gtf` is provided without `-genes`, all protein-coding genes from the annotation are considered.

### Optional Run Flags

- `-rna` Enable RNA transcript-based filtering
- `-or` Use OR logic for paired-end filtering

Default behavior without `-or` is AND logic for paired reads.

### Optional Output Flags

- `-fastq` Write filtered FASTQ outputs
- `-tsv` Write read-to-gene assignments
- `-counts` Write per-gene count summary

### Optional SNP Augmentation

- `-snp` Path to a directory containing SNP k-mer TSVs

SNP file format:

- one TSV per gene
- filename must be `<gene_id>.tsv`
- first line is a header
- first column contains k-mers

### Performance Parameters

- `-threads` Number of worker threads (default: `5`)
- `-chunksize` Reads per chunk (default: `50000`)

## Input File Expectations

- FASTQ files: paired-end reads in matching order
- FASTA file: reference sequence compatible with chromosome naming in GTF, in gz format
- GTF file: standard annotation with gene and transcript features, in gz format
- gene list file: one gene ID per line

## Output

Depending on selected output flags, CIKS7 writes files in `-od`:

- filtered FASTQ files
- read-to-gene TSV mappings
- count summary files

## Workflow of the Tool

1. Build a k-mer index from reference gene sequences.
2. Split FASTQ files into chunks for parallel processing.
3. Generate k-mers from each read.
4. Compare read k-mers against the gene index.
5. Retain reads that meet the specified threshold.
6. Write filtered reads and gene counts to the output directory.
   

## Recommended Parameters

Based on our evaluation:

- `k = 15`
- `offset = k`
- `threshold ≈ 75–105`
- `threads = 5-10`

These settings provide a good balance between performance and filtering accuracy.


## Citation

If you use CIKS7 in your research, please cite:

Moloney A., Leitz B.  
*CIKS7 – Counting and Identification using K-mer based Selection.*
