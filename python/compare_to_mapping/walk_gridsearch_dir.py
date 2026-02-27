import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import os
import re
import subprocess
import sys

import pandas as pd


def _find_matrix_dirs(gridsearch_dir):
    matrix_dirs = []
    for root, _, files in os.walk(gridsearch_dir):
        if "read2gene_matrix.tsv" in files:
            matrix_dirs.append(root)
    return sorted(matrix_dirs)


def _find_summary_files(gridsearch_dir):
    summary_files = []
    for root, _, files in os.walk(gridsearch_dir):
        if "comparison_summary.tsv" in files:
            summary_files.append(os.path.join(root, "comparison_summary.tsv"))
    return sorted(summary_files)


def _extract_param(pattern, rel_path):
    match = re.search(pattern, rel_path)
    if not match:
        return None
    return int(match.group(1))


def _extract_grid_params(gridsearch_dir, summary_file):
    rel_path = os.path.relpath(os.path.dirname(summary_file), gridsearch_dir)
    k = _extract_param(r"(?:^|/)k_(\d+)(?:/|$)", rel_path)
    offset = _extract_param(r"(?:^|/)offset_(\d+)(?:/|$)", rel_path)
    threshold = _extract_param(r"(?:^|/)threshold_(\d+)(?:/|$)", rel_path)
    return k, offset, threshold


def _run_single_comparison(matrix_dir, read_lists_dir, compare_script):
    filter_result = os.path.join(matrix_dir, "read2gene_matrix.tsv")
    cmd = [
        sys.executable,
        compare_script,
        "--filter-result",
        filter_result,
        "--read-lists",
        read_lists_dir,
        "--od",
        matrix_dir,
    ]

    try:
        subprocess.run(cmd, check=True, capture_output=True, text=True)
        return True, matrix_dir, ""
    except subprocess.CalledProcessError as exc:
        details = ""
        if exc.stdout:
            details += exc.stdout.strip() + "\n"
        if exc.stderr:
            details += exc.stderr.strip()
        return False, matrix_dir, details.strip()


def create_comparison_summaries(gridsearch_dir, read_lists_dir, compare_script, threads):
    matrix_dirs = _find_matrix_dirs(gridsearch_dir)
    if not matrix_dirs:
        print("No read2gene_matrix.tsv files found in the gridsearch directory.")
        return 0, 0

    created = 0
    failed = 0
    total = len(matrix_dirs)

    with ThreadPoolExecutor(max_workers=threads) as executor:
        future_to_dir = {
            executor.submit(_run_single_comparison, matrix_dir, read_lists_dir, compare_script): matrix_dir
            for matrix_dir in matrix_dirs
        }

        for done_idx, future in enumerate(as_completed(future_to_dir), start=1):
            success, matrix_dir, details = future.result()
            if success:
                created += 1
                print(f"Done ({done_idx}/{total}): {matrix_dir}")
            else:
                failed += 1
                print(f"Failed ({done_idx}/{total}): {matrix_dir}")
                if details:
                    print(details)

    return created, failed


def combine_comparison_summaries(gridsearch_dir):
    summary_files = _find_summary_files(gridsearch_dir)
    if not summary_files:
        print("No comparison_summary.tsv files found in the directory.")
        return None

    all_rows = []
    for summary_file in summary_files:
        k, offset, threshold = _extract_grid_params(gridsearch_dir, summary_file)
        df = pd.read_csv(summary_file, sep="\t")
        df["k"] = k
        df["offset"] = offset
        df["threshold"] = threshold
        df["summary_file"] = os.path.relpath(summary_file, gridsearch_dir)
        all_rows.append(df)

    combined_df = pd.concat(all_rows, ignore_index=True)
    combined_df = combined_df[[
        "k",
        "offset",
        "threshold",
        "gene",
        "filtered_reads",
        "mapping_reads",
        "matched",
        "not_mapped",
        "not_filtered",
        "summary_file",
    ]]
    combined_df.sort_values(["k", "offset", "threshold", "gene"], inplace=True)

    output_path = os.path.join(gridsearch_dir, "combined_comparison_summary_long.tsv")
    combined_df.to_csv(output_path, sep="\t", index=False)
    print(f"Combined long-format summary saved to {output_path}")
    print(f"Rows: {len(combined_df)} from {len(summary_files)} summary files")
    return output_path


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Run compare_filter_to_mapping.py for every gridsearch folder containing "
            "read2gene_matrix.tsv, then combine all comparison_summary.tsv files into "
            "a long-format summary with k/offset/threshold columns."
        )
    )
    parser.add_argument(
        "--gridsearch-dir",
        required=True,
        help="Path to the gridsearch root directory (contains k_*/offset_*/threshold_* folders)",
    )
    parser.add_argument(
        "--compare-script",
        default=os.path.join(os.path.dirname(__file__), "compare_filter_to_mapping.py"),
        help="Path to compare_filter_to_mapping.py",
    )
    parser.add_argument(
        "--threads",
        type=int,
        default=1,
        help="Number of parallel threads for comparison_summary generation (default: 1)",
    )
    args = parser.parse_args()

    gridsearch_dir = os.path.abspath(args.gridsearch_dir)
    read_lists_dir = os.path.join(gridsearch_dir, "read-lists")
    compare_script = os.path.abspath(args.compare_script)
    threads = args.threads

    if not os.path.isdir(gridsearch_dir):
        raise FileNotFoundError(f"Gridsearch directory not found: {gridsearch_dir}")
    if not os.path.isdir(read_lists_dir):
        raise FileNotFoundError(f"Read-lists directory not found: {read_lists_dir}")
    if not os.path.isfile(compare_script):
        raise FileNotFoundError(f"Compare script not found: {compare_script}")
    if threads <= 0:
        raise ValueError(f"--threads must be >= 1, got: {threads}")

    print(f"Gridsearch dir: {gridsearch_dir}")
    print(f"Read-lists dir: {read_lists_dir}")
    print(f"Compare script: {compare_script}")
    print(f"Threads: {threads}")

    created, failed = create_comparison_summaries(gridsearch_dir, read_lists_dir, compare_script, threads)
    print(f"comparison_summary.tsv generated: {created}, failed: {failed}")

    combine_comparison_summaries(gridsearch_dir)


if __name__ == "__main__":
    main()

    