package blockteil.filters;

import blockteil.Config;
import java.util.BitSet;

public abstract class KMERFilterer {
    
    public BitSet filterKMER(String fw, String rw) {
        BitSet fwResult = filterKMER(fw);
        BitSet rwResult = filterKMER(rw);
        if (Config.OR) {
            fwResult.or(rwResult);
        } else {
            fwResult.and(rwResult);
        }

        for (int gene = fwResult.nextSetBit(0); gene >= 0; gene = fwResult.nextSetBit(gene + 1)) {
            Config.COUNT_ARRAY.incrementAndGet(gene);
        }

        return fwResult;
    }
    public abstract BitSet filterKMER(String seq);
}