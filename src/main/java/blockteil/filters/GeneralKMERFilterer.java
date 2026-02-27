package blockteil.filters;

import blockteil.Config;
import blockteil.KMER;

import java.util.BitSet;
import java.util.Set;

public class GeneralKMERFilterer extends KMERFilterer {
    public BitSet filterKMER(String seq) {
        BitSet[] geneToMatchedPositions = new BitSet[Config.GENE_ARRAY.length];
        long kmer = 0;
        // Iterate over all k-mers and mark all matching positions for each gene
        A: for (int i = 0; i < seq.length() - Config.KMER_LENGTH + 1; i += Config.OFFSET) {
            if (Config.OFFSET == 1 && i > 0) kmer = KMER.shiftKMER(kmer, seq.charAt(i + Config.KMER_LENGTH - 1));
            else kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Config.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                BitSet matched = geneToMatchedPositions[gene];
                if (matched == null) {
                    matched = new BitSet();
                    geneToMatchedPositions[gene] = matched;
                }
                matched.set(i, i + Config.KMER_LENGTH);
                if (Config.EARLY_TERMINATION_ALLOWED && matched.cardinality() >= Config.THRESHOLD) break A;
            }
        }

        // Now determine which genes match the threshold and mark them in the output BitSet
        BitSet out = new BitSet(Config.GENE_ARRAY.length);
        for (int gene = 0; gene < geneToMatchedPositions.length; gene++) {
            BitSet matched = geneToMatchedPositions[gene];
            int matchedCount = matched == null ? 0 : matched.cardinality();
            if (matchedCount >= Config.THRESHOLD) {
                out.set(gene);
            }
        }
        return out;
    }
}
