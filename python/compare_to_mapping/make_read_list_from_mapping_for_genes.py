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

def make_rna_transcript_to_regions_dict(gtf_file, gene_list_file):
    gtf = pr.read_gtf(gtf_file).df

    with open(gene_list_file, 'r') as f:
        genes_of_interest = set(line.strip() for line in f)

    exon_rows = gtf[
        (gtf.Feature == "exon") &
        (gtf.transcript_id.notna()) &
        (gtf.gene_id.isin(genes_of_interest))
    ]

    transcripts2regions = {}

    for transcript_id, group in exon_rows.groupby("transcript_id"):

        chroms = group.Chromosome.unique()
        if len(chroms) != 1:
            print(f"Warning: Transcript {transcript_id} has exons on multiple chromosomes; skipping.")
            continue

        chrom = chroms[0]
        regions = sorted(
            [(int(row.Start), int(row.End)) for _, row in group.iterrows()],
            key=lambda x: x[0],
        )
        transcript_start = min(start for start, _ in regions)
        transcript_end = max(end for _, end in regions)
        transcripts2regions[transcript_id] = (chrom, transcript_start, transcript_end, regions)

    return transcripts2regions

def write_gene_read_list_fetch(
    bamfile,
    chrom,
    start,
    end,
    regions,
    gene_name,
    output_dir,
    clip_threshold,
    pair_mode,
):
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"{gene_name}_reads.txt")
    pair_state = {}

    for read in bamfile.fetch(chrom, start, end):
        read_id = normalize_read_id(read.query_name)
        passed = not skip_read(read, chrom, regions, clip_threshold)

        if read_id not in pair_state:
            pair_state[read_id] = {
                "pass_read1": False,
                "pass_read2": False,
            }

        state = pair_state[read_id]

        if read.is_paired:
            if read.is_read1:
                state["pass_read1"] = state["pass_read1"] or passed
            elif read.is_read2:
                state["pass_read2"] = state["pass_read2"] or passed

    read_ids = set()
    for read_id, state in pair_state.items():
        if pair_mode == "and":
            if state["pass_read1"] and state["pass_read2"]:
                read_ids.add(read_id)
        else:
            if state["pass_read1"] or state["pass_read2"]:
                read_ids.add(read_id)

    with open(output_file, 'w') as f:
        for rid in sorted(read_ids):
            f.write(f"{rid}\n")

def skip_read(read, chrom, regions, threshold=105):
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
    if out_of_bounds(read, regions):
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


def out_of_bounds(read, regions):
    for block_start, block_end in read.get_blocks():
        inside_any_region = any(
            block_start >= region_start and block_end <= region_end
            for region_start, region_end in regions
        )
        if not inside_any_region:
            return True
    return False

def main(args):
    if args.rna:
        transcripts2regions = make_rna_transcript_to_regions_dict(args.gtf, args.gene_list)
    else:
        genes2positions = make_gene_to_position_dict(args.gtf, args.gene_list)
    bamfile = pysam.AlignmentFile(args.mapping, "rb")

    clip_thresholds = args.clip_thresholds if args.clip_thresholds else [args.clip_threshold]
    pair_modes = ["and", "or"]

    for clip_threshold in clip_thresholds:
        threshold_output_dir = os.path.join(args.od, f"threshold_{clip_threshold}")

        for pair_mode in pair_modes:
            mode_output_dir = os.path.join(threshold_output_dir, pair_mode)
            print(
                f"Creating read lists for clipping threshold {clip_threshold}, "
                f"pair mode {pair_mode} in {mode_output_dir}"
            )

            if args.rna:
                iterator = (
                    (transcript_id, chrom, start, end, regions)
                    for transcript_id, (chrom, start, end, regions) in transcripts2regions.items()
                )
            else:
                iterator = (
                    (gene_id, chrom, start, end, [(start, end)])
                    for gene_id, (chrom, start, end) in genes2positions.items()
                )

            for item_name, chrom, start, end, regions in iterator:

                write_gene_read_list_fetch(
                    bamfile,
                    chrom,
                    start,
                    end,
                    regions,
                    item_name,
                    mode_output_dir,
                    clip_threshold,
                    pair_mode,
                )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create a read list from a mapping file for specific genes.")
    parser.add_argument("--mapping", default="data/pig-data-rnaseq/mapped/minimap2/H5-12939-T2.sorted.bam", help="Path to the mapping file (BAM).")
    parser.add_argument("--gene-list", required=True, help="Path to the file containing the list of genes.")
    parser.add_argument("--gtf", default="data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz", help="Path to the GTF file containing gene annotations." )
    parser.add_argument("--od", required=True, help="Path to the output directory where the read lists will be saved (one for each gene).")
    parser.add_argument("--rna", action="store_true", help="Indicates that the transcripts should be considered, not genes.")
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