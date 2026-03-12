#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAR="${JAR:-gta_filter.jar}"
FW="${FW:-data/pig-data-rnaseq/H5-12939-T2_R1_001.fastq.gz}"
RW="${RW:-data/pig-data-rnaseq/H5-12939-T2_R3_001.fastq.gz}"
FASTA="${FASTA:-data/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz}"
GTF="${GTF:-data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz}"
GENES="${GENES:-output/plotting_data/snp/gene_list.txt}"
OUT_BASE="${OUT_BASE:-output/plotting_data/snp/rna}"
MAX_PARALLEL="1"

TIME_SUMMARY="$OUT_BASE/time_summary.tsv"

declare -a K_VALUES=(15)

declare -a THRESHOLD_VALUES=(75 90 105 120)
declare -a PAIR_MODES=(and or)

join_by() {
  local IFS="$1"
  shift
  echo "$*"
}

total_runs=0
for k in "${K_VALUES[@]}"; do
  for threshold in "${THRESHOLD_VALUES[@]}"; do
    for _pair_mode in "${PAIR_MODES[@]}"; do
      total_runs=$((total_runs + 1))
    done
  done
done
run_idx=0
executed_runs=0

echo -e "k\toffset\tthreshold\tpair_mode\tmode\tsource_threshold\telapsed\tuser_sec\tsystem_sec\tmax_rss_kb\ttime_file" > "$TIME_SUMMARY"

extract_time_metric() {
  local key="$1"
  local file="$2"
  awk -F': ' -v k="$key" 'index($0, k) {print $NF; exit}' "$file"
}

run_one() {
  local k="$1"
  local offset="$2"
  local threshold="$3"
  local pair_mode="$4"
  local out_dir="$5"

  local time_file="$out_dir/time_verbose.txt"
  local run_log="$out_dir/run.log"
  local row_file="$out_dir/time_row.tsv"

  local -a cmd=(
    java -jar "$JAR"
    -fw "$FW"
    -rw "$RW"
    -k "$k"
    -offset "$offset"
    -threshold "$threshold"
    -fasta "$FASTA"
    -od "$out_dir"
    -gtf "$GTF"
    -genes "$GENES"
    -tsv
    -counts
    -snp output/kmers_pig
    -rna
  )

  if [[ "$pair_mode" == "or" ]]; then
    cmd+=( -or )
  fi

  /usr/bin/time -v -o "$time_file" "${cmd[@]}" > "$run_log" 2>&1

  local elapsed
  local user_sec
  local system_sec
  local max_rss
  elapsed="$(extract_time_metric "Elapsed (wall clock) time (h:mm:ss or m:ss)" "$time_file")"
  user_sec="$(extract_time_metric "User time (seconds)" "$time_file")"
  system_sec="$(extract_time_metric "System time (seconds)" "$time_file")"
  max_rss="$(extract_time_metric "Maximum resident set size (kbytes)" "$time_file")"

  echo -e "${k}\t${offset}\t${threshold}\t${pair_mode}\texecuted\t-\t${elapsed:-NA}\t${user_sec:-NA}\t${system_sec:-NA}\t${max_rss:-NA}\t${time_file}" > "$row_file"
}

throttle_jobs() {
  local -n pids_ref=$1
  while (( ${#pids_ref[@]} >= MAX_PARALLEL )); do
    wait -n
    local alive=()
    for pid in "${pids_ref[@]}"; do
      if kill -0 "$pid" 2>/dev/null; then
        alive+=("$pid")
      fi
    done
    pids_ref=("${alive[@]}")
  done
}

wait_for_jobs() {
  local -n pids_ref=$1
  for pid in "${pids_ref[@]}"; do
    wait "$pid"
  done
  pids_ref=()
}

write_global_summary() {
  {
    echo -e "k\toffset\tthreshold\tpair_mode\tmode\tsource_threshold\telapsed\tuser_sec\tsystem_sec\tmax_rss_kb\ttime_file"
    find "$OUT_BASE" -type f -name "time_row.tsv" | sort | while IFS= read -r row; do
      cat "$row"
    done
  } > "$TIME_SUMMARY"
}

echo "Starting grid search"
echo "K: $(join_by , "${K_VALUES[@]}")"
echo "Offset: k"
echo "Threshold: $(join_by , "${THRESHOLD_VALUES[@]}")"
echo "Pair mode: $(join_by , "${PAIR_MODES[@]}")"
echo "Parallel Java runs: up to $MAX_PARALLEL"
echo "Total runs: $total_runs"
echo "Time summary: $TIME_SUMMARY"

for k in "${K_VALUES[@]}"; do
  pids=()
  offset="$k"

  for threshold in "${THRESHOLD_VALUES[@]}"; do
    for pair_mode in "${PAIR_MODES[@]}"; do
      run_idx=$((run_idx + 1))
      out_dir="$OUT_BASE/k_${k}/offset_${offset}/threshold_${threshold}/mode_${pair_mode}"
      mkdir -p "$out_dir"

      throttle_jobs pids

      echo "[$run_idx/$total_runs] EXECUTE k=$k offset=$offset threshold=$threshold pair_mode=$pair_mode"
      run_one "$k" "$offset" "$threshold" "$pair_mode" "$out_dir" &
      pids+=("$!")
      executed_runs=$((executed_runs + 1))
    done
  done

  wait_for_jobs pids
done

write_global_summary

echo "Grid search finished. Results are in: $OUT_BASE"
echo "Executed runs: $executed_runs"

python/venv/bin/python3.11 python/compare_to_mapping/walk_gridsearch_dir.py --gridsearch-dir "$OUT_BASE" --threads 2