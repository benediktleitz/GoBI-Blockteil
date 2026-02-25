package blockteil;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.OutOfMemoryError;
import java.lang.Runtime;

public class Main {

    public static  int OFFSET;
    //public static final HashMap<Long, HashSet<Integer>> KMER_MAP = new HashMap<>();
    public static final Long2ObjectOpenHashMap<IntOpenHashSet> KMER_MAP = new Long2ObjectOpenHashMap<>();
    public static String[] GENE_ARRAY = null;
    public static int THRESHOLD = 0;
    public static int KMER_LENGTH;

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        Runtime run = Runtime.getRuntime();
        System.out.println(run.maxMemory());
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
        
        ReferenceKMERSetCreator creator = new ReferenceKMERSetCreator(cmd.getOptionValue("fasta"));

        String chr = cmd.getOptionValue("chr");
        String startStr = cmd.getOptionValue("start");
        String endStr = cmd.getOptionValue("end");
        String genes = cmd.getOptionValue("genes");
        String gtf = cmd.getOptionValue("gtf");

        if (chr != null && startStr != null && endStr != null) {
            int start = 0, end = 0;
            try {
                start = Integer.parseInt(startStr);
                end = Integer.parseInt(endStr);
            } catch (NumberFormatException e) {
                System.err.println("Invalid start or end position: " + startStr + ", " + endStr);
                System.exit(1);
            }
            Gene g = new Gene(null, chr, start, end);
            creator.addKMERS(g);
        } else if (genes != null && gtf != null) {
            Einleseroutine reader = new Einleseroutine(gtf);
            reader.read();
            creator.addKMERS(genes, reader.id2gene);
        } else if (gtf != null){
            try {
                Einleseroutine reader = new Einleseroutine(gtf);
                reader.read();
                creator.addKMERS(null, reader.id2gene); // no genes list -> all genes in GTF file
            } catch (OutOfMemoryError e) {
                System.err.println("Error in adding k-mers: " + e.getMessage());
                System.out.println(KMER_MAP.size() + " unique k-mers in map, " + GENE_ARRAY.length + " genes in array");
                System.exit(1);
            }
        } else {
            System.err.println("Either chr, start and end, or genes and gtf, or only gtf (for all protein coding genes) options must be provided");
            System.exit(1);
        }
        System.out.println("Finished creating k-mer map, starting to filter reads...");
        System.out.println(KMER_MAP.size() + " unique k-mers in map, " + GENE_ARRAY.length + " genes in array");
        
        String fw = cmd.getOptionValue("fw");
        String rw = cmd.getOptionValue("rw");
        String od = cmd.getOptionValue("od");

        Path outPath = Path.of(od, "filtered_genes.tsv");
        Path countPath = Path.of(od, "gene_counts.tsv");
        if (fw == null || rw == null) {
            System.err.println("Both fw and rw options must be provided");
            System.exit(1);
        }
        ReadEinleseroutine.filterReads(fw, rw, outPath);
        ReadEinleseroutine.writeGeneCounts(countPath);
        
    }

    public int add(int a, int b) {
        return a + b;
    }
    public void printHelp() {
        System.out.println("HEEEEEEEEELP!");
    }
}
