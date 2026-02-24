package blockteil;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class KMERFilterer {
    
    public static BitSet filterKMER(String fw, String rw) {
        BitSet fwResult = filterKMER(fw);
        BitSet rwResult = filterKMER(rw);
        fwResult.and(rwResult); // TODO: maybe add option for OR vs AND
        return fwResult;
    }

    private static BitSet filterKMER(String seq) {
        Map<Integer, BitSet> geneToMatchedPositions = new HashMap<>();
        long kmer;
        // Iterate over all k-mers and mark all matching positions for each gene
        for (int i = 0; i < seq.length() - Main.KMER_LENGTH + 1; i += Main.OFFSET) {
            if (Main.OFFSET == 1 && i > 0) kmer = KMER.shiftKMER(kmer, seq.charAt(i + Main.KMER_LENGTH - 1));
            else kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Main.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                BitSet matched = geneToMatchedPositions.computeIfAbsent(gene, k -> new BitSet());
                matched.set(i, i + Main.KMER_LENGTH);
            }
        }

        // Now determine which genes match the threshold and mark them in the output BitSet
        BitSet out = new BitSet(Main.GENE_ARRAY.length);
        for (Map.Entry<Integer, BitSet> entry : geneToMatchedPositions.entrySet()) {
            int gene = entry.getKey();
            BitSet matched = entry.getValue();
            int matchedCount = matched.cardinality();
            if (matchedCount >= Main.THRESHOLD) {
                out.set(gene);
            }
        }
        return out;
    }
}