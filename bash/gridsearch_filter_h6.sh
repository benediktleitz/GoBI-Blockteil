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
OUT_BASE="${OUT_BASE:-output/filter_quality_analysis/H5/gridsearch1}"
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

declare -a OFFSETS=()
for o in $(seq 1 5 30); do
  OFFSETS+=("$o")
done

declare -a K_VALUES=()
for k in $(seq 5 5 30); do
  K_VALUES+=("$k")
done

declare -a THRESHOLDS=()
for t in $(seq 10 20 110); do
  THRESHOLDS+=("$t")
done

total_runs=$(( ${#K_VALUES[@]} * ${#OFFSETS[@]} * ${#THRESHOLDS[@]} ))
run_idx=0
executed_runs=0
copied_runs=0

echo -e "k\toffset\tthreshold\tmode\tsource_threshold\telapsed\tuser_sec\tsystem_sec\tmax_rss_kb\ttime_file" > "$TIME_SUMMARY"

extract_time_metric() {
  local key="$1"
  local file="$2"
  awk -F': ' -v k="$key" '$0 ~ k {print $2; exit}' "$file"
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
      > "$run_log" 2>&1

  local elapsed
  local user_sec
  local system_sec
  local max_rss
  elapsed="$(extract_time_metric "Elapsed (wall clock) time" "$time_file")"
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

  echo -e "${k}\t${offset}\t${threshold}\tcopied\t${src_threshold}\tNA\tNA\tNA\tNA\t$src_dir/time_verbose.txt" > "$out_dir/time_row.tsv"
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
echo "K: 5,10,15,20,25,30"
echo "Offset: 3,6,9,...,30"
echo "Threshold: 10..100 step 10"
echo "Parallel Java runs: up to $MAX_PARALLEL"
echo "Total runs: $total_runs"
echo "Time summary: $TIME_SUMMARY"

for k in "${K_VALUES[@]}"; do
  for offset in "${OFFSETS[@]}"; do
    executed_threshold=""
    executed_dir=""
    copy_jobs=()
    pids=()

    for threshold in "${THRESHOLDS[@]}"; do
      run_idx=$((run_idx + 1))
      out_dir="$OUT_BASE/k_${k}/offset_${offset}/threshold_${threshold}"
      mkdir -p "$out_dir"

      if (( threshold <= k )); then
        if [[ -z "$executed_threshold" ]]; then
          echo "[$run_idx/$total_runs] EXECUTE k=$k offset=$offset threshold=$threshold"
          throttle_jobs pids
          run_one "$k" "$offset" "$threshold" "$out_dir" &
          pids+=("$!")
          executed_threshold="$threshold"
          executed_dir="$out_dir"
          executed_runs=$((executed_runs + 1))
        else
          echo "[$run_idx/$total_runs] COPY    k=$k offset=$offset threshold=$threshold (from threshold=$executed_threshold, deferred)"
          copy_jobs+=("${threshold}:${out_dir}")
          copied_runs=$((copied_runs + 1))
        fi
      else
        echo "[$run_idx/$total_runs] EXECUTE k=$k offset=$offset threshold=$threshold"
        throttle_jobs pids
        run_one "$k" "$offset" "$threshold" "$out_dir" &
        pids+=("$!")
        executed_runs=$((executed_runs + 1))
      fi
    done

    wait_for_jobs pids

    for job in "${copy_jobs[@]}"; do
      threshold="${job%%:*}"
      out_dir="${job#*:}"
      copy_equivalent_outputs "$executed_dir" "$out_dir" "$executed_threshold"
      append_copied_row "$k" "$offset" "$threshold" "$executed_threshold" "$executed_dir" "$out_dir"
      {
        echo "k=${k}"
        echo "offset=${offset}"
        echo "threshold=${threshold}"
        echo "mode=copied"
        echo "source_threshold=${executed_threshold}"
        echo "elapsed=NA"
        echo "user_sec=NA"
        echo "system_sec=NA"
        echo "max_rss_kb=NA"
        echo "time_file=$executed_dir/time_verbose.txt"
        echo "run_log=$executed_dir/run.log"
      } > "$out_dir/time_summary.txt"
    done
  done
done

write_global_summary

echo "Grid search finished. Results are in: $OUT_BASE"
echo "Executed runs: $executed_runs"
echo "Copied runs:   $copied_runs"
