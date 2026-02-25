package blockteil;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class Config {
    public static  final int OFFSET;
    //public static final HashMap<Long, HashSet<Integer>> KMER_MAP = new HashMap<>();
    public static final Long2ObjectOpenHashMap<IntOpenHashSet> KMER_MAP = new Long2ObjectOpenHashMap<>();
    public static final String[] GENE_ARRAY;
    public static final int THRESHOLD;
    public static final int KMER_LENGTH;
    public static final boolean EARLY_TERMINATION_ALLOWED;

    public static init(int kmerLength, int offset, int threshold) {
        if (kmerLength <= 0 || kmerLength > 31) {
            throw new IllegalArgumentException("k-mer size must be between 1 and 31");
        }
        if (offset <= 0) {
            throw new IllegalArgumentException("Offset must be a positive integer");
        }
        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be a positive integer");
        }
        KMER.init();
    }

    public static setGeneArray(String[] geneArray) {
        GENE_ARRAY = geneArray;
    }

    public static setEarlyTerminationAllowed(boolean allowed) {
        EARLY_TERMINATION_ALLOWED = allowed;
    }
}
