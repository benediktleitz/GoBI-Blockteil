#!/usr/bin/env bash
# Runtime Überlegungen:
# Gerade: 8 k values, 5 t values, 8 o values -> 21 base settings
# Pro setting and/or und dna/rna und 4 runs -> 21 * 3 = 63 runs
# 4 Fastq files mit insgesamt 250 Mio reads
# 13 Mio reads pro minute -> 250/13 = 20 min pro run
# 63 runs * 20 min = 1260 min = 21 h = 0.88 Tage


set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

OUT_BASE="output/chunksize_analysis"


mkdir -p "$OUT_BASE"

FASTA_MNT="/mnt/raidbio2/extdata/praktikum/genprakt/genprakt-ws25/Block/pig-genome/Sus_scrofa.Sscrofa11.1.dna.toplevel.fa.gz"
GTF_MNT="/mnt/raidbio2/extdata/praktikum/genprakt/genprakt-ws25/Block/pig-genome/Sus_scrofa.Sscrofa11.1.115.chr.gtf.gz"
GENES_MNT="/mnt/biocluster/praktikum/genprakt/gruppe_e/gene_list.txt"
JAR_MNT="/mnt/biocluster/praktikum/genprakt/leitzb/blockteil/GoBI-Blockteil/gta_filter.jar"

FASTA="$FASTA_MNT"
GTF="$GTF_MNT"
GENES="$GENES_MNT"
JAR="$JAR_MNT"

FASTQ_BASE="/mnt/raidbio2/extdata/praktikum/genprakt/genprakt-ws25/Block/pig-data-rnaseq"
TIME_SUMMARY="$OUT_BASE/time_summary.tsv"

declare -a CHUNKSIZE_VALUES=()
for chunksize in $(seq 25000 25000 200000); do
  CHUNKSIZE_VALUES+=("$chunksize")
done
declare -a PAIR_MODES=(or)
declare -i RUNS_PER_SETTING=3
declare -a SAMPLES=(H9 H3 H2 H6)

declare -i K_FIXED=15
declare -i OFFSET_FIXED=15
declare -i THRESHOLD_FIXED=105

declare -i run_idx=0
declare -i total_runs=$(( ${#CHUNKSIZE_VALUES[@]} * RUNS_PER_SETTING * ${#SAMPLES[@]} ))
declare -i executed_runs=0
 
echo "Runtime chunk analysis configuration:"
echo "  fixed parameters: k=${K_FIXED}, offset=${OFFSET_FIXED}, threshold=${THRESHOLD_FIXED}"
echo "  chunksizes: 25000..200000 (step 25000), runs per chunksize: ${RUNS_PER_SETTING}"
echo "  samples: ${SAMPLES[*]}"
echo "  planned runs: ${total_runs}"
echo "Waiting 4 hours before starting..."
sleep 15000


echo -e "k\toffset\tthreshold\tchunksize\tpair_mode\tmode\tsample\telapsed\tuser_sec\tsystem_sec\tmax_rss_kb\tpercent_cpu" > "$TIME_SUMMARY"

extract_time_metric() {
  local key="$1"
  local file="$2"
  awk -F': ' -v k="$key" 'index($0, k) {print $NF; exit}' "$file"
}

write_time_summary() {
  local time_file="$1"
  local mode="$2"
  local sample="$3"
  local k="$4"
  local offset="$5"
  local threshold="$6"
  local chunksize="$7"
  local pair_mode="$8"
  
  local elapsed
  local user_sec
  local system_sec
  local max_rss
  local percent_cpu
  elapsed="$(extract_time_metric "Elapsed (wall clock) time (h:mm:ss or m:ss)" "$time_file")"
  user_sec="$(extract_time_metric "User time (seconds)" "$time_file")"
  system_sec="$(extract_time_metric "System time (seconds)" "$time_file")"
  max_rss="$(extract_time_metric "Maximum resident set size (kbytes)" "$time_file")"
  percent_cpu="$(extract_time_metric "Percent of CPU this job got" "$time_file")"

  echo -e "${k}\t${offset}\t${threshold}\t${chunksize}\t${pair_mode}\t${mode}\t${sample}\t${elapsed:-NA}\t${user_sec:-NA}\t${system_sec:-NA}\t${max_rss:-NA}\t${percent_cpu:-NA}" >> "$TIME_SUMMARY"
}

run_one() {
  local k="$1"
  local offset="$2"
  local threshold="$3"
  local pair_mode="$4"
  local chunksize="$5"
  local out_dir="$6"

  local sample=$(basename "$FW" _R1_001.fastq.gz)

  local time_file="$out_dir/time_verbose.txt"

  local -a cmd=(
    java -jar "$JAR"
    -fw "$FW"
    -rw "$RW"
    -k "$k"
    -offset "$offset"
    -threshold "$threshold"
    -chunksize "$chunksize"
    -fasta "$FASTA"
    -od "$out_dir"
    -gtf "$GTF"
    -genes "$GENES"
    -counts
  )

  if [[ "$pair_mode" == "or" ]]; then
    cmd+=( -or )
  fi

  # DNA mode run
  /usr/bin/time -v -o "$time_file" "${cmd[@]}"

  write_time_summary "$time_file" "dna" "$sample" "$k" "$offset" "$threshold" "$chunksize" "$pair_mode"
}

for sample in "${SAMPLES[@]}"; do

  r1=$(printf "%s\n" "$FASTQ_BASE"/${sample}-*_R1_001.fastq.gz | head -n1)
  prefix=$(basename "$r1" _R1_001.fastq.gz)

  FW="$r1"
  RW="${FASTQ_BASE}/${prefix}_R3_001.fastq.gz"

  if [[ ! -f "$FW" || ! -f "$RW" ]]; then
    echo "WARNING: missing FASTQ files $FW or $RW — skipping sample"
    continue
  fi

  for chunksize in "${CHUNKSIZE_VALUES[@]}"; do
    for pair_mode in "${PAIR_MODES[@]}"; do
      for r in $(seq 1 "$RUNS_PER_SETTING"); do
        run_idx=$((run_idx + 1))

        echo "[$run_idx/$total_runs] EXECUTE k=${K_FIXED} offset=${OFFSET_FIXED} threshold=${THRESHOLD_FIXED} chunksize=${chunksize} pair_mode=$pair_mode"
        run_one "$K_FIXED" "$OFFSET_FIXED" "$THRESHOLD_FIXED" "$pair_mode" "$chunksize" "$OUT_BASE"
        executed_runs=$((executed_runs + 1))
      done
    done
  done

done

echo "Run time analysis finished. Results are in: $OUT_BASE"
echo "Executed runs: $executed_runs"