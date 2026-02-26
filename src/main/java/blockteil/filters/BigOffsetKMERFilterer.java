package blockteil.filters;

import blockteil.Config;
import blockteil.KMER;

import java.util.BitSet;
import java.util.Set;

public class BigOffsetKMERFilterer extends KMERFilterer {
    public BitSet filterKMER(String seq) {
        int[] geneToMatchedPositions = new int[Config.GENE_ARRAY.length];
        long kmer = 0;

        // Iterate over all k-mers and adding Main.KMER_LENGTH to the matched count for each gene
        A: for (int i = 0; i < seq.length() - Config.KMER_LENGTH + 1; i += Config.OFFSET) {
            kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Config.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                if (geneToMatchedPositions[gene] == 0) Config.COUNT_ARRAY[gene] += 1;
                geneToMatchedPositions[gene] += Config.KMER_LENGTH;
                if (Config.EARLY_TERMINATION_ALLOWED && geneToMatchedPositions[gene] >= Config.THRESHOLD) break A;
            }
        }

        // Now determine which genes match the threshold and mark them in the output BitSet
        BitSet out = new BitSet(Config.GENE_ARRAY.length);
        for (int gene = 0; gene < geneToMatchedPositions.length; gene++) {
            int matched = geneToMatchedPositions[gene];
            if (matched >= Config.THRESHOLD) {
                out.set(gene);
            }
        }
        return out;
    }
}
