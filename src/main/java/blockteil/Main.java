package blockteil;

import java.util.HashMap;
import java.util.HashSet;

public class Main {

    public static final int KMER_LENGTH;
    public static final int OFFSET;
    public static final HashMap<Long, HashSet<Integer>> KMER_MAP = new HashMap<>();
    public static final String[] GENE_ARRAY;
    public static final int THRESHOLD;

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        CmdLineReader cmd = new CmdLineReader(args);
        try {
            KMER_LENGTH = Integer.parseInt(cmd.getOptionValue("k"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid k-mer size: " + cmd.getOptionValue("k"));
            System.exit(1);
        }
        if (KMER_LENGTH <= 0 || KMER_LENGTH > 31) {
            System.err.println("k-mer size must be between 1 and 31");
            System.exit(1);
        }
    }

    public int add(int a, int b) {
        return a + b;
    }
}
