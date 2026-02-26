import pandas as pd
import pysam
import argparse
import pyranges as pr
import os

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

def write_gene_read_list(bamfile, chrom, start, end, gene_name, output_dir):
    output_file = os.path.join(output_dir, f"{gene_name}_reads.txt")
    os.makedirs(output_dir, exist_ok=True)
    with open(output_file, 'w') as f:
        for column in bamfile.pileup(chrom, start, end):
            for read in column.pileups:
                if not read.is_del and not read.is_refskip:
                    f.write(f"{read.alignment.query_name}\n")

def write_gene_read_list_fetch(bamfile, chrom, start, end, gene_name, output_dir):
    os.makedirs(output_dir, exist_ok=True)
    output_file = os.path.join(output_dir, f"{gene_name}_reads.txt")
    with open(output_file, 'w') as f:
        for read in bamfile.fetch(chrom, start, end):
            f.write(f"{read.query_name}\n")

def main(args):
    genes2positions = make_gene_to_position_dict(args.gtf, args.gene_list)
    bamfile = pysam.AlignmentFile(args.mapping, "rb")
    
    for gene, (chrom, start, end) in genes2positions.items():
        write_gene_read_list(bamfile, chrom, start, end, gene, args.od)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create a read list from a mapping file for specific genes.")
    parser.add_argument("--mapping", required=True, help="Path to the mapping file (BAM).")
    parser.add_argument("--gene_list", required=True, help="Path to the file containing the list of genes.")
    parser.add_argument("--gtf", required=True, help="Path to the GTF file containing gene annotations." )
    parser.add_argument("--od", required=True, help="Path to the output directory where the read lists will be saved (one for each gene).")
    
    args = parser.parse_args()
    main(args)