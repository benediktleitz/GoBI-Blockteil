import argparse
import os

from walk_gridsearch_dir import combine_comparison_summaries


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Combine existing comparison_summary.tsv files in a gridsearch directory "
            "into combined_comparison_summary_long.tsv."
        )
    )
    parser.add_argument(
        "--gridsearch-dir",
        required=True,
        help="Path to the gridsearch root directory.",
    )
    args = parser.parse_args()

    gridsearch_dir = os.path.abspath(args.gridsearch_dir)
    if not os.path.isdir(gridsearch_dir):
        raise FileNotFoundError(f"Gridsearch directory not found: {gridsearch_dir}")

    output_path = combine_comparison_summaries(gridsearch_dir)
    if output_path is None:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
