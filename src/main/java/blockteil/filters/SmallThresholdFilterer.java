package blockteil.filters;

import blockteil.Config;
import blockteil.KMER;

import java.util.BitSet;
import java.util.Set;

public class SmallThresholdFilterer extends KMERFilterer {

    public BitSet filterKMER(String seq) {
        BitSet out = new BitSet(Config.GENE_ARRAY.length);
        long kmer = 0;

        // Iterate over all k-mers and mark all matching genes (since here 1 k-mer match is enough to pass the threshold)
        A: for (int i = 0; i < seq.length() - Config.KMER_LENGTH + 1; i += Config.OFFSET) {
            if (Config.OFFSET == 1 && i > 0) kmer = KMER.shiftKMER(kmer, seq.charAt(i + Config.KMER_LENGTH - 1));
            else kmer = KMER.makeKMER(seq, i);

            Set<Integer> matchingGenes = Config.KMER_MAP.get(kmer);
            if (matchingGenes == null) continue;

            for (Integer gene : matchingGenes) {
                out.set(gene);
                Config.COUNT_ARRAY[gene] += 1;
                if (Config.EARLY_TERMINATION_ALLOWED) break A;
            }
        }
        return out;
    }
    
}
