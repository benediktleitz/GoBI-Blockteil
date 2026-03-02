package blockteil.filters;

import blockteil.Config;
import java.util.BitSet;

public abstract class KMERFilterer {
    
    public BitSet filterKMER(String fw, String rw) {
        BitSet fwResult = filterKMER(fw, false);
        BitSet fwResult_revcomp = filterKMER(fw, true);
        BitSet rwResult = filterKMER(rw, false);
        BitSet rwResult_revcomp = filterKMER(rw, true);
        if (Config.OR) {
            fwResult.or(rwResult_revcomp);
            fwResult_revcomp.or(rwResult);
        } else {
            fwResult.and(rwResult_revcomp);
            fwResult_revcomp.and(rwResult);
        }
        // We take the maximum of genes, but allow only one of the two orientations to contribute to the count (since they come from the same read)
        //fwResult = fwResult.cardinality() >= fwResult_revcomp.cardinality() ? fwResult : fwResult_revcomp;
        fwResult.or(fwResult_revcomp); // -> Alternatively, we could also take the union of the two orientations, but then we would have to count all matches in both orientations, which might lead to more false positives (since a read can only come from one orientation)
        if (Config.WRITE_COUNT) {
            for (int gene = fwResult.nextSetBit(0); gene >= 0; gene = fwResult.nextSetBit(gene + 1)) {
                Config.COUNT_ARRAY.incrementAndGet(gene);
            }
        }

        return fwResult;
    }
    public abstract BitSet filterKMER(String seq, boolean revcomp);
}