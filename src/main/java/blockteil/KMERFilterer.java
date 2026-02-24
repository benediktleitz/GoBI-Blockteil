package blockteil;

import java.util.BitSet;
import java.util.Set;

public class KMERFilterer {
    
    public static BitSet filterKMER(String fw, String rw) {
        BitSet fwResult;
        BitSet rwResult;
        if (Main.THRESHOLD <= Main.KMER_LENGTH) {
            fwResult = filterKMERSmallTreshold(fw);
            rwResult = filterKMERSmallTreshold(rw);
        } else if (Main.OFFSET >= Main.KMER_LENGTH) {
            fwResult = filterKMERBigOffset(fw);
            rwResult = filterKMERBigOffset(rw);
        } else {
            fwResult = filterKMER(fw);
            rwResult = filterKMER(rw);
        }
        fwResult.and(rwResult); // TODO: maybe add option for OR vs AND
        return fwResult;
    }

    private static BitSet filterKMER(String seq) {
        BitSet[] geneToMatchedPositions = new BitSet[Main.GENE_ARRAY.length];
        long kmer = 0;
        // Iterate over all k-mers and mark all matching positions for each gene
        for (int i = 0; i < seq.length() - Main.KMER_LENGTH + 1; i += Main.OFFSET) {
            if (Main.OFFSET == 1 && i > 0) kmer = KMER.shiftKMER(kmer, seq.charAt(i + Main.KMER_LENGTH - 1));
            else kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Main.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                BitSet matched = geneToMatchedPositions[gene];
                if (matched == null) {
                    matched = new BitSet();
                    geneToMatchedPositions[gene] = matched;
                }
                matched.set(i, i + Main.KMER_LENGTH);
            }
        }

        // Now determine which genes match the threshold and mark them in the output BitSet
        BitSet out = new BitSet(Main.GENE_ARRAY.length);
        for (int gene = 0; gene < geneToMatchedPositions.length; gene++) {
            BitSet matched = geneToMatchedPositions[gene];
            int matchedCount = matched == null ? 0 : matched.cardinality();
            if (matchedCount >= Main.THRESHOLD) {
                out.set(gene);
            }
        }
        return out;
    }

    private static BitSet filterKMERSmallTreshold(String seq) {
        BitSet out = new BitSet(Main.GENE_ARRAY.length);
        long kmer = 0;

        // Iterate over all k-mers and mark all matching genes (since here 1 k-mer match is enough to pass the threshold)
        for (int i = 0; i < seq.length() - Main.KMER_LENGTH + 1; i += Main.OFFSET) {
            if (Main.OFFSET == 1 && i > 0) kmer = KMER.shiftKMER(kmer, seq.charAt(i + Main.KMER_LENGTH - 1));
            else kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Main.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                out.set(gene);
            }
        }
        return out;
    }

    private static BitSet filterKMERBigOffset(String seq) {
        int[] geneToMatchedPositions = new int[Main.GENE_ARRAY.length];
        long kmer = 0;

        // Iterate over all k-mers and adding Main.KMER_LENGTH to the matched count for each gene
        for (int i = 0; i < seq.length() - Main.KMER_LENGTH + 1; i += Main.OFFSET) {
            kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Main.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                geneToMatchedPositions[gene] += Main.KMER_LENGTH;
            }
        }

        // Now determine which genes match the threshold and mark them in the output BitSet
        BitSet out = new BitSet(Main.GENE_ARRAY.length);
        for (int gene = 0; gene < geneToMatchedPositions.length; gene++) {
            int matched = geneToMatchedPositions[gene];
            if (matched >= Main.THRESHOLD) {
                out.set(gene);
            }
        }
        return out;
    }

}