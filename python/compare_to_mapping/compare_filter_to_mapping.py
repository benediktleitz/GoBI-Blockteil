import pandas as pd
import argparse
import os
import re


def normalize_read_id(read_id):
    rid = str(read_id).strip()
    rid = rid.split(" ")[0]
    if rid.startswith("@"):
        rid = rid[1:]
    return rid

def compare_filter_to_mapping(gene, filter_df, read_lists_dir):
    gene_rows = filter_df[filter_df[gene] == "1"]
    filter_reads = set(gene_rows["read_id"].map(lambda x: normalize_read_id(x)))
    read_list_file = os.path.join(read_lists_dir, f"{gene}_reads.txt")
    if not os.path.exists(read_list_file):
        print(f"Warning: Read list file for gene {gene} not found at {read_list_file}. Skipping.")
        return (gene, len(filter_reads), 0, 0, 0, 0)
    
    with open(read_list_file, 'r') as f:
        mapping_reads = set(normalize_read_id(line) for line in f if line.strip())
    
    matched = len(filter_reads.intersection(mapping_reads))
    not_mapped = len(filter_reads - mapping_reads)
    not_filtered = len(mapping_reads - filter_reads)
    
    return (gene, len(filter_reads), len(mapping_reads), matched, not_mapped, not_filtered)

def main(args):
    filter_df = pd.read_csv(args.filter_result, sep='\t', dtype=str)
    filter_df.fillna("0", inplace=True)
    filter_df["read_id"] = filter_df["read_id"].map(lambda x: normalize_read_id(x))

    genes = filter_df.columns[1:]
    summary_data = []
    for gene in genes:
        summary_data.append(compare_filter_to_mapping(gene, filter_df, args.read_lists))

    output_path = os.path.abspath(os.path.join(args.od, "comparison_summary.tsv"))
    with open(output_path, 'w') as f:
        f.write("gene\tfiltered_reads\tmapping_reads\tmatched\tnot_mapped\tnot_filtered\n")
        for row in summary_data:
            f.write("\t".join(map(str, row)) + "\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Create a summary file for comparison from read lists for mapping results vs filter tsv for specific genes.")
    parser.add_argument("--filter-result", required=True, help="Path to the filter result tsv file")
    parser.add_argument("--read-lists", required=True, help="Path to the directory containing the list of reads per gene (one file per gene).")
    parser.add_argument("--od", required=True, help="Path to the output directory where the summary output will be saved.")
    
    args = parser.parse_args()
    main(args)