import pysam
import argparse
import pyranges as pr
import os


def normalize_read_id(read_id):
    rid = str(read_id).strip()
    rid = rid.split(" ")[0]
    if rid.startswith("@"):
        rid = rid[1:]
    return rid

def make_gene_to_position_dict(gtf_file, gene_list_file):
    gtf = pr.read_gtf(gtf_file)
    with open(gene_list_file, 'r') as f:
        genes_of_interest = set(line.strip() for line in f)
    genes2positions = {}
    for gene_id in genes_of_interest:
        gene_info = gtf[gtf.gene_id == gene_id]
        if len(gene_info) > 0:
            chrom = gene_info.Chromosome.iloc[0]  
            start = gene_info.Start.iloc[0]      
            end = gene_info.End.iloc[0]    
            genes2positions[gene_id] = (chrom, start, end)
        else:
            print(f"Warning: Gene {gene_id} not found in GTF file.")
    return genes2positions

def make_transcript_to_position_dict(gtf_file, gene_list_file):
    gtf = pr.read_gtf(gtf_file).df

    with open(gene_list_file, 'r') as f:
        genes_of_interest = set(line.strip() for line in f)

    transcript_rows = gtf[
        (gtf.Feature == "transcript") &
        (gtf.gene_id.isin(genes_of_interest))
    ]

    transcripts2positions = {}

    for _, row in transcript_rows.iterrows():
        transcripts2positions[row.transcript_id] = (
            row.Chromosome,
            row.Start,
            row.End
        )

    return transcripts2positions

def write_gene_read_list_pileup(bamfile, chrom, start, end, gene_name, output_dir):
    output_file = os.path.join(output_dir, f"{gene_name}_reads.txt")
    os.makedirs(output_dir, exist_ok=True)
    read_ids = set()
    for column in bamfile.pileup(chrom, start, end):
        for read in column.pileups:
            if not read.is_del and not read.is_refskip:
                read_ids.add(normalize_read_id(read.alignment.query_name))

    with open(output_file, 'w') as f:
        for rid in sorted(read_ids):
            f.write(f"{rid}\n")

def write_gene_read_list_fetch(bamfile, chrom, start, end, gene_name, output_dir, clip_threshold):
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"{gene_name}_reads.txt")
    read_ids = set()
    for read in bamfile.fetch(chrom, start, end):
        if skip_read(read, chrom, clip_threshold):
            continue
        read_ids.add(normalize_read_id(read.query_name))

    with open(output_file, 'w') as f:
        for rid in sorted(read_ids):
            f.write(f"{rid}\n")

def skip_read(read, chrom, threshold=105):
    if read.is_unmapped:
        return True
    if read.is_secondary:
        return True
    if read.is_supplementary:
        return True
    if read.is_duplicate:
        return True
    if read.is_qcfail:
        return True
    if read.mapping_quality < 20:
        return True
    if read.reference_name != chrom:
        return True
    if not read.is_proper_pair:
        return True
    if read.next_reference_name != chrom:
        return True
    if clipped_bases(read) > threshold:
        return True
    
    return False

def clipped_bases(read):
    if read.cigartuples is None:
        return 0
    clipped = 0
    for op, length in read.cigartuples:
        if op in (4, 5):  # 4: soft clip, 5: hard clip
            clipped += length
    return clipped

def main(args):
    if args.rna:
        transcripts2positions = make_transcript_to_position_dict(args.gtf, args.gene_list)
        positions = transcripts2positions
    else:
        genes2positions = make_gene_to_position_dict(args.gtf, args.gene_list)
        positions = genes2positions
    bamfile = pysam.AlignmentFile(args.mapping, "rb")

    clip_thresholds = args.clip_thresholds if args.clip_thresholds else [args.clip_threshold]

    for clip_threshold in clip_thresholds:
        threshold_output_dir = args.od
        if len(clip_thresholds) > 1:
            threshold_output_dir = os.path.join(args.od, f"threshold_{clip_threshold}")

        print(f"Creating read lists for clipping threshold {clip_threshold} in {threshold_output_dir}")

        for gene, (chrom, start, end) in positions.items():
            if args.method == "pileup":
                write_gene_read_list_pileup(
                    bamfile,
                    chrom,
                    start,
                    end,
                    gene,
                    threshold_output_dir,
                )
            else:
                write_gene_read_list_fetch(
                    bamfile,
                    chrom,
                    start,
                    end,
                    gene,
                    threshold_output_dir,
                    clip_threshold,
                )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create a read list from a mapping file for specific genes.")
    parser.add_argument("--mapping", required=True, help="Path to the mapping file (BAM).")
    parser.add_argument("--gene-list", required=True, help="Path to the file containing the list of genes.")
    parser.add_argument("--gtf", required=True, help="Path to the GTF file containing gene annotations." )
    parser.add_argument("--od", required=True, help="Path to the output directory where the read lists will be saved (one for each gene).")
    parser.add_argument("--rna", action="store_true", help="Indicates that the transcripts should be considered, not genes.")
    parser.add_argument(
        "--method",
        choices=["fetch", "pileup"],
        default="fetch",
        help="Method used to collect reads from BAM (default: fetch).",
    )
    parser.add_argument(
        "--clip-threshold",
        type=int,
        default=105,
        help="Maximum allowed clipped bases per read for fetch mode (default: 105).",
    )
    parser.add_argument(
        "--clip-thresholds",
        type=int,
        nargs="+",
        help=(
            "Optional list of clipping thresholds. If provided, read lists are generated "
            "in subfolders named threshold_<value> below --od."
        ),
    )
    
    args = parser.parse_args()
    main(args)