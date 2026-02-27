package blockteil.filters;

import java.io.ObjectInputFilter.Config;
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
        return fwResult;
    }
    public abstract BitSet filterKMER(String seq);
}