package blockteil;

import blockteil.filters.*;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class Config {
    public static int OFFSET;
    public static final Long2ObjectOpenHashMap<IntOpenHashSet> KMER_MAP = new Long2ObjectOpenHashMap<>();
    public static String[] GENE_ARRAY;
    public static int[] COUNT_ARRAY;
    public static int THRESHOLD;
    public static int KMER_LENGTH;
    public static boolean EARLY_TERMINATION_ALLOWED;
    public static boolean WRITE_FASTQ;
    public static boolean WRITE_COUNT;
    public static boolean WRITE_TSV;
    public static KMERFilterer KMER_FILTERER;

    public static void init(CmdLineReader cmd) {
        try {
            KMER_LENGTH = Integer.parseInt(cmd.getOptionValue("k"));
            OFFSET = Integer.parseInt(cmd.getOptionValue("offset"));
            THRESHOLD = Integer.parseInt(cmd.getOptionValue("threshold"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid k-mer size, or offset, or threshold: " + cmd.getOptionValue("k") + ", " + cmd.getOptionValue("offset") + ", " + cmd.getOptionValue("threshold"));
            System.exit(1);
        }
        if (KMER_LENGTH <= 0 || KMER_LENGTH > 31) {
            throw new IllegalArgumentException("k-mer size must be between 1 and 31");
        }
        if (OFFSET <= 0) {
            throw new IllegalArgumentException("Offset must be a positive integer");
        }
        if (THRESHOLD <= 0) {
            throw new IllegalArgumentException("Threshold must be a positive integer");
        }

        WRITE_FASTQ = cmd.hasOption("fastq");
        WRITE_COUNT = cmd.hasOption("counts");
        WRITE_TSV = cmd.hasOption("tsv");
        EARLY_TERMINATION_ALLOWED = !WRITE_COUNT && !WRITE_TSV;

        if(THRESHOLD <= KMER_LENGTH) KMER_FILTERER = new SmallThresholdFilterer();
        else if(OFFSET >= KMER_LENGTH) KMER_FILTERER = new BigOffsetKMERFilterer();
        else KMER_FILTERER = new GeneralKMERFilterer();
    }

    public static void setGeneArray(String[] geneArray) {
        GENE_ARRAY = geneArray;
        COUNT_ARRAY = new int[geneArray.length];
        if (geneArray.length == 1) {
            EARLY_TERMINATION_ALLOWED = true; // if only one gene, we can terminate early as soon as we find a match
        }
    }
}
