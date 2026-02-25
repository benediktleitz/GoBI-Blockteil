package blockteil.filters;

import blockteil.Config;

import java.util.BitSet;
import java.util.Set;

public abstract class KMERFilterer {
    
    public BitSet filterKMER(String fw, String rw) {
        BitSet fwResult = filterKMER(fw);
        BitSet rwResult = filterKMER(rw);

        fwResult.and(rwResult); // TODO: maybe add option for OR vs AND
        return fwResult;
    }
    public abstract BitSet filterKMER(String seq);
}