import pandas as pd
import argparse
import os


def normalize_read_id_series(series):
    return (
        series.astype(str)
        .str.strip()
        .str.split(" ").str[0]
        .str.lstrip("@")
    )


def compare_filter_to_mapping(gene, filter_sets, read_lists_dir):
    filter_reads = filter_sets.get(gene, set())

    read_list_file = os.path.join(read_lists_dir, f"{gene}_reads.txt")
    if not os.path.exists(read_list_file):
        print(f"Warning: Read list file for gene {gene} not found at {read_list_file}. Skipping.")
        return (gene, len(filter_reads), 0, 0, 0, 0)

    with open(read_list_file, "r") as f:
        mapping_reads = {
            line.strip().split(" ")[0].lstrip("@")
            for line in f if line.strip()
        }

    matched = len(filter_reads & mapping_reads)
    not_mapped = len(filter_reads - mapping_reads)
    not_filtered = len(mapping_reads - filter_reads)

    return (gene, len(filter_reads), len(mapping_reads),
            matched, not_mapped, not_filtered)


def main(args):
    filter_df = pd.read_csv(
        args.filter_result,
        sep="\t",
        dtype=str,
        engine="pyarrow"  # falls back automatically if not installed
    ).fillna("0")

    filter_df["read_id"] = normalize_read_id_series(filter_df["read_id"])

    long_df = (
        filter_df
        .set_index("read_id")
        .stack()
        .reset_index()
    )

    long_df.columns = ["read_id", "gene", "value"]

    # keep only 1s
    long_df = long_df[long_df["value"] == "1"]

    filter_sets = (
        long_df.groupby("gene")["read_id"]
        .apply(set)
        .to_dict()
    )

    genes = filter_df.columns[1:]
    summary_data = []

    for gene in genes:
        summary_data.append(
            compare_filter_to_mapping(gene, filter_sets, args.read_lists)
        )

    output_path = os.path.abspath(
        os.path.join(args.od, "comparison_summary.tsv")
    )

    with open(output_path, "w") as f:
        f.write("gene\tfiltered_reads\tmapping_reads\tmatched\tnot_mapped\tnot_filtered\n")
        for row in summary_data:
            f.write("\t".join(map(str, row)) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Create a summary file for comparison from read lists for mapping results vs filter tsv for specific genes."
    )
    parser.add_argument("--filter-result", required=True,
                        help="Path to the filter result tsv file")
    parser.add_argument("--read-lists", required=True,
                        help="Path to the directory containing the list of reads per gene (one file per gene).")
    parser.add_argument("--od", required=True,
                        help="Path to the output directory where the summary output will be saved.")

    args = parser.parse_args()
    main(args)