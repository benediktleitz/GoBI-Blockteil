#!/usr/bin/env bash

shopt -s nullglob

gridsearchDir="${1:-${GRIDSEARCH_DIR:-output/plotting_data/quality/rna-contained/}}"
summaryFile="${gridsearchDir}/totalCountSummary3_excl.tsv"
excludedGene1="ENSSSCG00000018081"
excludedGene2="ENSSSCG00000035362"

if [[ ! -d "$gridsearchDir" ]]; then
    echo "Error: gridsearch directory not found: $gridsearchDir" >&2
    exit 1
fi

readListsRoot="${gridsearchDir}/read-lists"
if [[ ! -d "$readListsRoot" ]]; then
    echo "Error: read-lists directory not found: $readListsRoot" >&2
    exit 1
fi

fullReadsDir="${gridsearchDir}/full-read-lists"
mkdir -p "$fullReadsDir"

declare -A mappedSetByThresholdMode

for thresholdDir in "$readListsRoot"/threshold_*; do
    [[ -d "$thresholdDir" ]] || continue
    threshold="${thresholdDir##*/}"
    threshold="${threshold#threshold_}"

    for modeDir in "$thresholdDir"/*; do
        [[ -d "$modeDir" ]] || continue
        mode="${modeDir##*/}"

        fullReadsFile="${fullReadsDir}/FullReads_threshold_${threshold}_${mode}.txt"
        if compgen -G "$modeDir/*.txt" > /dev/null; then
            tmpMappedRaw=$(mktemp)
            : > "$tmpMappedRaw"

            for readListFile in "$modeDir"/*.txt; do
                [[ -f "$readListFile" ]] || continue

                baseName="${readListFile##*/}"
                geneName="${baseName%.txt}"
                geneName="${geneName%_reads}"

                if [[ -n "$excludedGene1" && "$geneName" == "$excludedGene1" ]]; then
                    continue
                fi
                if [[ -n "$excludedGene2" && "$geneName" == "$excludedGene2" ]]; then
                    continue
                fi

                awk -F'\t' '{print $1}' "$readListFile" >> "$tmpMappedRaw"
            done

            sort -u "$tmpMappedRaw" > "$fullReadsFile"
            rm -f "$tmpMappedRaw"
        else
            : > "$fullReadsFile"
        fi

        key="${threshold}|${mode}"
        mappedSetByThresholdMode["$key"]="$fullReadsFile"
    done
done

echo -e "k\toffset\tthreshold\tmode\ttotalMapped\ttotalRetained\tmatchedReads\tmapped_not_retained" > "$summaryFile"

for kdir in "$gridsearchDir"/k_*; do
    [[ -d "$kdir" ]] || continue
    k="${kdir##*/}"
    k="${k#k_}"

    for odir in "$kdir"/offset_*; do
        [[ -d "$odir" ]] || continue
        offset="${odir##*/}"
        offset="${offset#offset_}"

        for tdir in "$odir"/threshold_*; do
            [[ -d "$tdir" ]] || continue
            threshold="${tdir##*/}"
            threshold="${threshold#threshold_}"

            for mdir in "$tdir"/mode_*; do
                [[ -d "$mdir" ]] || continue
                mode="${mdir##*/}"
                mode="${mode#mode_}"

                matrixFile="$mdir/read2gene_matrix.tsv"
                [[ -f "$matrixFile" ]] || continue

                key="${threshold}|${mode}"
                mappedSet="${mappedSetByThresholdMode[$key]}"
                if [[ -z "$mappedSet" || ! -f "$mappedSet" ]]; then
                    echo "Warning: missing mapped read-list set for threshold=${threshold}, mode=${mode}" >&2
                    continue
                fi

                tmpRetained=$(mktemp)
                if [[ -z "$excludedGene1" && -z "$excludedGene2" ]]; then
                    awk -F'\t' 'NR>1 {print $1}' "$matrixFile" | sort -u > "$tmpRetained"
                else
                    awk -F'\t' -v gene1="$excludedGene1" -v gene2="$excludedGene2" '
                        NR==1 {
                            for (i=2; i<=NF; i++) {
                                if ((gene1 != "" && $i == gene1) || (gene2 != "" && $i == gene2)) {
                                    excludedCols[i] = 1
                                }
                            }
                            next
                        }

                        NR>1 {
                            hasExcluded = 0
                            hasIncluded = 0

                            for (i=2; i<=NF; i++) {
                                value = $i + 0
                                if (value != 0) {
                                    if (i in excludedCols) {
                                        hasExcluded = 1
                                    } else {
                                        hasIncluded = 1
                                        break
                                    }
                                }
                            }

                            if (hasIncluded || !hasExcluded) {
                                print $1
                            }
                        }
                    ' "$matrixFile" | sort -u > "$tmpRetained"
                fi

                totalMapped=$(wc -l < "$mappedSet")
                totalRetained=$(wc -l < "$tmpRetained")
                matchedReads=$(comm -12 "$mappedSet" "$tmpRetained" | wc -l)
                mappedNotRetained=$(comm -23 "$mappedSet" "$tmpRetained" | wc -l)

                echo -e "${k}\t${offset}\t${threshold}\t${mode}\t${totalMapped}\t${totalRetained}\t${matchedReads}\t${mappedNotRetained}" >> "$summaryFile"

                rm -f "$tmpRetained"
            done
        done
    done
done

