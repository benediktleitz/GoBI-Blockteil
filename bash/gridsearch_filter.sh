#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAR="${JAR:-gta_filter.jar}"
FW="${FW:-data/pig-data-rnaseq/H5-12939-T2_R1_001.fastq.gz}"
RW="${RW:-data/pig-data-rnaseq/H5-12939-T2_R3_001.fastq.gz}"
FASTA="${FASTA:-data/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz}"
GTF="${GTF:-data/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz}"
GENES="${GENES:-output/filter_quality_analysis/H5/gridsearch1/gene_list.txt}"
OUT_BASE="${OUT_BASE:-output/filter_quality_analysis/H5/gridsearch2}"
MAX_PARALLEL="${MAX_PARALLEL:-8}"

if [[ ! -f "$JAR" ]]; then
  echo "ERROR: JAR not found at '$JAR'"
  exit 1
fi
if [[ ! -f "$FW" || ! -f "$RW" || ! -f "$FASTA" || ! -f "$GTF" || ! -f "$GENES" ]]; then
  echo "ERROR: One or more required input files are missing."
  echo "FW=$FW"
  echo "RW=$RW"
  echo "FASTA=$FASTA"
  echo "GTF=$GTF"
  echo "GENES=$GENES"
  exit 1
fi

mkdir -p "$OUT_BASE"
TIME_SUMMARY="$OUT_BASE/time_summary.tsv"

declare -a K_VALUES=()
for k in $(seq 12 3 30); do
  K_VALUES+=("$k")
done

join_by() {
  local IFS="$1"
  shift
  echo "$*"
}

total_runs=0
for k in "${K_VALUES[@]}"; do
  for threshold in $(seq $((3 * k)) "$k" 150); do
    total_runs=$((total_runs + 1))
  done
done
run_idx=0
executed_runs=0
copied_runs=0

echo -e "k\toffset\tthreshold\tmode\tsource_threshold\telapsed\tuser_sec\tsystem_sec\tmax_rss_kb\ttime_file" > "$TIME_SUMMARY"

extract_time_metric() {
  local key="$1"
  local file="$2"
  awk -F': ' -v k="$key" 'index($0, k) {print $NF; exit}' "$file"
}

run_one() {
  local k="$1"
  local offset="$2"
  local threshold="$3"
  local out_dir="$4"

  local time_file="$out_dir/time_verbose.txt"
  local run_log="$out_dir/run.log"
  local per_run_summary="$out_dir/time_summary.txt"
  local row_file="$out_dir/time_row.tsv"

  /usr/bin/time -v -o "$time_file" \
    java -jar "$JAR" \
      -fw "$FW" \
      -rw "$RW" \
      -k "$k" \
      -offset "$offset" \
      -threshold "$threshold" \
      -fasta "$FASTA" \
      -od "$out_dir" \
      -gtf "$GTF" \
      -genes "$GENES" \
      -tsv \
      -counts \
      -rna \
      > "$run_log" 2>&1

  local elapsed
  local user_sec
  local system_sec
  local max_rss
  elapsed="$(extract_time_metric "Elapsed (wall clock) time (h:mm:ss or m:ss)" "$time_file")"
  user_sec="$(extract_time_metric "User time (seconds)" "$time_file")"
  system_sec="$(extract_time_metric "System time (seconds)" "$time_file")"
  max_rss="$(extract_time_metric "Maximum resident set size (kbytes)" "$time_file")"

  echo -e "${k}\t${offset}\t${threshold}\texecuted\t-\t${elapsed:-NA}\t${user_sec:-NA}\t${system_sec:-NA}\t${max_rss:-NA}\t${time_file}" > "$row_file"

  {
    echo "k=${k}"
    echo "offset=${offset}"
    echo "threshold=${threshold}"
    echo "mode=executed"
    echo "source_threshold=-"
    echo "elapsed=${elapsed:-NA}"
    echo "user_sec=${user_sec:-NA}"
    echo "system_sec=${system_sec:-NA}"
    echo "max_rss_kb=${max_rss:-NA}"
    echo "time_file=${time_file}"
    echo "run_log=${run_log}"
  } > "$per_run_summary"
}

append_copied_row() {
  local k="$1"
  local offset="$2"
  local threshold="$3"
  local src_threshold="$4"
  local src_dir="$5"
  local out_dir="$6"
  local src_row="$src_dir/time_row.tsv"
  local elapsed="NA"
  local user_sec="NA"
  local system_sec="NA"
  local max_rss="NA"

  if [[ -f "$src_row" ]]; then
    elapsed="$(awk -F'\t' 'NR==1 {print $6}' "$src_row")"
    user_sec="$(awk -F'\t' 'NR==1 {print $7}' "$src_row")"
    system_sec="$(awk -F'\t' 'NR==1 {print $8}' "$src_row")"
    max_rss="$(awk -F'\t' 'NR==1 {print $9}' "$src_row")"
  fi

  echo -e "${k}\t${offset}\t${threshold}\tcopied\t${src_threshold}\t${elapsed}\t${user_sec}\t${system_sec}\t${max_rss}\t$src_dir/time_verbose.txt" > "$out_dir/time_row.tsv"
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
    echo -e "k\toffset\tthreshold\tmode\tsource_threshold\telapsed\tuser_sec\tsystem_sec\tmax_rss_kb\ttime_file"
    find "$OUT_BASE" -type f -name "time_row.tsv" | sort | while IFS= read -r row; do
      cat "$row"
    done
  } > "$TIME_SUMMARY"
}

copy_equivalent_outputs() {
  local src_dir="$1"
  local dst_dir="$2"
  local src_threshold="$3"

  cp "$src_dir/read2gene_matrix.tsv" "$dst_dir/read2gene_matrix.tsv"
  cp "$src_dir/gene_counts.tsv" "$dst_dir/gene_counts.tsv"
  printf "copied_from_threshold=%s\n" "$src_threshold" > "$dst_dir/copied_from.txt"
}

echo "Starting grid search"
echo "K: $(join_by , "${K_VALUES[@]}")"
echo "Offset: k (for each k)"
echo "Threshold: n*k for n>=3 and threshold<=150"
echo "Parallel Java runs: up to $MAX_PARALLEL"
echo "Total runs: $total_runs"
echo "Time summary: $TIME_SUMMARY"

for k in "${K_VALUES[@]}"; do
  offset="$k"
  pids=()

  for threshold in $(seq $((3 * k)) "$k" 150); do
    run_idx=$((run_idx + 1))
    out_dir="$OUT_BASE/k_${k}/offset_${offset}/threshold_${threshold}"
    mkdir -p "$out_dir"

    echo "[$run_idx/$total_runs] EXECUTE k=$k offset=$offset threshold=$threshold"
    throttle_jobs pids
    run_one "$k" "$offset" "$threshold" "$out_dir" &
    pids+=("$!")
    executed_runs=$((executed_runs + 1))
  done

  wait_for_jobs pids
done

write_global_summary

echo "Grid search finished. Results are in: $OUT_BASE"
echo "Executed runs: $executed_runs"
echo "Copied runs:   $copied_runs"
