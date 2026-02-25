package blockteil;

import filters.KMERFilterer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class Config {
    public static int OFFSET;
    //public static final HashMap<Long, HashSet<Integer>> KMER_MAP = new HashMap<>();
    public static final Long2ObjectOpenHashMap<IntOpenHashSet> KMER_MAP = new Long2ObjectOpenHashMap<>();
    public static String[] GENE_ARRAY;
    public static int THRESHOLD;
    public static int KMER_LENGTH;
    public static boolean EARLY_TERMINATION_ALLOWED;
    public static boolean WRITE_FASTQ;
    public static boolean WRITE_COUNT;
    public static boolean WRITE_TSV;
    public static KMERFilterer KMER_FILTERER;

    public static void init(int kmerLength, int offset, int threshold) {
        if (kmerLength <= 0 || kmerLength > 31) {
            throw new IllegalArgumentException("k-mer size must be between 1 and 31");
        }
        KMER_LENGTH = kmerLength;
        if (offset <= 0) {
            throw new IllegalArgumentException("Offset must be a positive integer");
        }
        OFFSET = offset;
        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be a positive integer");
        }
        THRESHOLD = threshold;
        KMER.init();
    }

    public static void setGeneArray(String[] geneArray) {
        GENE_ARRAY = geneArray;
    }

    public static void setEarlyTerminationAllowed(boolean allowed) {
        EARLY_TERMINATION_ALLOWED = allowed;
    }
}
