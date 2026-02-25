package blockteil;

import java.util.HashMap;
import java.util.HashSet;

public class Main {

    public static  int OFFSET;
    public static final HashMap<Long, HashSet<Integer>> KMER_MAP = new HashMap<>();
    public static String[] GENE_ARRAY = null;
    public static int THRESHOLD = 0;
    public static int KMER_LENGTH;

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        CmdLineReader cmd = new CmdLineReader(args);
        try {
            KMER_LENGTH = Integer.parseInt(cmd.getOptionValue("k"));
            OFFSET = Integer.parseInt(cmd.getOptionValue("offset"));
            THRESHOLD = Integer.parseInt(cmd.getOptionValue("threshold"));
        } catch (NumberFormatException e) {
            System.err.println("Invalid k-mer size, or offset, or threshold: " + cmd.getOptionValue("k") + ", " + cmd.getOptionValue("offset") + ", " + cmd.getOptionValue("threshold"));
            System.exit(1);
        }
        if (KMER_LENGTH <= 0 || KMER_LENGTH > 31) {
            System.err.println("k-mer size must be between 1 and 31");
            System.exit(1);
        }
        if (OFFSET <= 0) {
            System.err.println("Offset must be a positive integer");
            System.exit(1);
        }
        if (THRESHOLD <= 0) {
            System.err.println("Threshold must be a positive integer");
            System.exit(1);
        }
        KMER.init();


    }

    public int add(int a, int b) {
        return a + b;
    }
    public void printHelp() {
        System.out.println("HEEEEEEEEELP!");
    }
}
