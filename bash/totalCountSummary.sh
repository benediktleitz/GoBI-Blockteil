#!/usr/bin/env bash

# read lists are grouped by threshold under read-lists/threshold_<t>/

gridsearchDir="output/plotting_data/quality/rna/"
excludedGene="${1:-all}"

if [[ "$excludedGene" == "all" || -z "$excludedGene" ]]; then
    excludedGene=""
    summaryFile="${gridsearchDir}/totalCountSummary3.tsv"
else
    summaryFile="${gridsearchDir}/totalCountSummary3_excluding_${excludedGene}.tsv"
fi

geneColumn=0

if [[ -n "$excludedGene" ]]; then
    firstMatrix=$(find "${gridsearchDir}" -path '*/mode_*/read2gene_matrix.tsv' | head -n 1)
    if [[ -n "$firstMatrix" ]]; then
        geneColumn=$(awk -F'\t' -v gene="$excludedGene" 'NR==1 {for (i=1; i<=NF; i++) if ($i == gene) {print i; exit}}' "$firstMatrix")
        geneColumn=${geneColumn:-0}
    fi

    if [[ "$geneColumn" -eq 0 ]]; then
        echo "Warning: gene ${excludedGene} not found in matrix headers. Running without exclusion." >&2
        excludedGene=""
    fi
fi

fullReadsDir="${gridsearchDir}/read-lists/FullReads"
mkdir -p "$fullReadsDir"

# Build one full-read file per threshold once.
for thresholdDir in "${gridsearchDir}"/read-lists/threshold_*; do
    [[ -d "$thresholdDir" ]] || continue

    thresholdName=$(basename "$thresholdDir")
    thresholdValue=${thresholdName#threshold_}
    fullReadsFile="${fullReadsDir}/FullReads_threshold_${thresholdValue}.txt"

    rm -f "$fullReadsFile"
    for f in "$thresholdDir"/*.txt; do
        [[ -f "$f" ]] || continue
        [[ "$(basename "$f")" == "FullReadList.txt" ]] && continue
        cat "$f" >> "$fullReadsFile"
    done
done

echo -e "mode\tk\toffset\tthreshold\ttotalFiltered\ttotalMapped\tfiltered_not_mapped\tmapped_not_filtered" > "$summaryFile"

for kdir in ${gridsearchDir}/k_*; do
    k=$(basename "$kdir" | cut -d_ -f2)

    for odir in ${kdir}/offset_*; do
        offset=$(basename "$odir" | cut -d_ -f2)

        for tdir in ${odir}/threshold_*; do
            threshold=$(basename "$tdir" | cut -d_ -f2)

            thresholdFullReads="${fullReadsDir}/FullReads_threshold_${threshold}.txt"
            if [[ ! -f "$thresholdFullReads" ]]; then
                echo "Warning: missing full reads file for threshold ${threshold}: ${thresholdFullReads}" >&2
                continue
            fi

            tmpMapped=$(mktemp)
            cut -f1 "$thresholdFullReads" | sort -u > "$tmpMapped"
            totalMapped=$(wc -l < "$tmpMapped")

            for mdir in ${tdir}/mode_*; do
                mode=$(basename "$mdir" | cut -d_ -f2)

                file="${mdir}/read2gene_matrix.tsv"

                if [[ -f "$file" ]]; then

                    tmpFiltered=$(mktemp)

                    # unique filtered reads
                    if [[ -z "$excludedGene" ]]; then
                        awk -F'\t' 'NR > 1 {print $1}' "$file" | sort -u > "$tmpFiltered"
                    else
                        awk -F'\t' -v col="$geneColumn" 'NR > 1 && ($(col) + 0) == 0 {print $1}' "$file" | sort -u > "$tmpFiltered"
                    fi

                    totalFiltered=$(wc -l < "$tmpFiltered")

                    # set differences
                    filtered_not_mapped=$(comm -23 "$tmpFiltered" "$tmpMapped" | wc -l)
                    mapped_not_filtered=$(comm -13 "$tmpFiltered" "$tmpMapped" | wc -l)

                    echo -e "${mode}\t${k}\t${offset}\t${threshold}\t${totalFiltered}\t${totalMapped}\t${filtered_not_mapped}\t${mapped_not_filtered}" >> "$summaryFile"

                    rm "$tmpFiltered"
                fi
            done

            rm "$tmpMapped"
        done
    done
done