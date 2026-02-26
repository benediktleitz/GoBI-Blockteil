package blockteil;


import blockteil.reference.*;
import blockteil.readprocessing.ReadEinleseroutine;
import blockteil.readprocessing.Writer;

import java.nio.file.Path;
import java.lang.OutOfMemoryError;


public class Main {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        CmdLineReader cmd = new CmdLineReader(args);

        Config.init(cmd);
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
                System.out.println(Config.KMER_MAP.size() + " unique k-mers in map, " + Config.GENE_ARRAY.length + " genes in array");
                System.exit(1);
            }
        } else {
            System.err.println("Either chr, start and end, or genes and gtf, or only gtf (for all protein coding genes) options must be provided");
            System.exit(1);
        }
        System.out.println("Finished creating k-mer map, starting to filter reads...");
        System.out.println(Config.KMER_MAP.size() + " unique k-mers in map, " + Config.GENE_ARRAY.length + " genes in array");
        
        String fw = cmd.getOptionValue("fw");
        String rw = cmd.getOptionValue("rw");
        String od = cmd.getOptionValue("od");

        Path outPath = Path.of(od, "filtered_genes.tsv");
        Path countPath = Path.of(od, "gene_counts.tsv");
        if (fw == null || rw == null) {
            System.err.println("Both fw and rw options must be provided");
            System.exit(1);
        }
        ReadEinleseroutine.filterReads(fw, rw, od);
        if (Config.WRITE_COUNT) {
            Writer.writeGeneCounts(countPath);
        }
        
    }

    public int add(int a, int b) {
        return a + b;
    }
    public void printHelp() {
        System.out.println("HEEEEEEEEELP!");
    }
}
