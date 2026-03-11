package blockteil;

import org.apache.commons.cli.*;


public class CmdLineReader {
    private CommandLine cmd;

    public CmdLineReader(String[] args) {
        Options options = get_parse_options();
        CommandLine cmd = null;

        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            System.err.println("Cmdline parsing failed.  Reason: " + e.getMessage());
            formatter.printHelp("java -jar Solution1.jar", options);
        }

        this.cmd = cmd;
    }

    public Options get_parse_options() {
        Options options = new Options();
        Option fw = Option.builder("fw")
                .hasArg()
                .desc("path to fw fastq file")
                .required()
                .build();

        Option rw = Option.builder("rw")
                .hasArg()
                .desc("path to rw fastq file")
                .required()
                .build();

        Option k = Option.builder("k")
                .hasArg()
                .desc("k-mer size")
                .required()
                .build();

        Option offset = Option.builder("offset")
                .hasArg()
                .desc("offset for read k-mer building")
                .required()
                .build();

        Option gtf = Option.builder("gtf")
                .hasArg()
                .desc("path to the GTF file")
                .build();

        Option fasta = Option.builder("fasta")
                .hasArg()
                .desc("genome FASTA file")
                .required()
                .build();

        Option genes = Option.builder("genes")
                .hasArg()
                .desc("list of genes to analyze, newline-separated")
                .build();

        Option output_dir = Option.builder("od")
                .hasArg()
                .desc("output directory")
                .required()
                .build();
         
        Option threshold = Option.builder("threshold")
                .hasArg()
                .desc("threshold value for filtering")
                .required()
                .build();
        
        Option chr = Option.builder("chr")
                .hasArg()
                .desc("chromosome name")
                .build();

        Option start = Option.builder("start")
                .hasArg()
                .desc("start position (1-based)")
                .build();

        Option end = Option.builder("end")
                .hasArg()
                .desc("end position (1-based)")
                .build();

        Option fastq = Option.builder("fastq")
                .desc("optional flag to write fastq files of filtered reads")
                .build();

        Option tsv = Option.builder("tsv")
                .desc("optional flag to write tsv file (filtered reads to gene matrix)")
                .build();

        Option counts = Option.builder("counts")
                .desc("optional flag to write gene counts file")
                .build();

        Option rna = Option.builder("rna")
                .desc("optional flag to test RNA-Seq data")
                .build();

        Option or = Option.builder("or")
                .desc("optional flag to use OR instead of AND for filtering to combine filtering of pair-end reads")
                .build();
        
        Option threads = Option.builder("threads")
                .hasArg()
                .desc("number of threads to use for read processing, default=5")
                .build();
        
        Option chunksize = Option.builder("chunksize")
                .hasArg()
                .desc("number of reads to process in each chunk, default=50000")
                .build();

        Option kmerMap = Option.builder("kmerMap")
                .hasArg()
                .desc("path to output kmer map file (for plotting purposes)")
                .build();
        
        Option snp = Option.builder("snp")
                .hasArg()
                .desc("path to SNP k-mer directory for additional k-mers to filter for")
                .build();


        options.addOption(fw);
        options.addOption(rw);
        options.addOption(k);
        options.addOption(offset);
        options.addOption(genes);
        options.addOption(fasta);
        options.addOption(gtf);
        options.addOption(output_dir);
        options.addOption(threshold);
        options.addOption(chr);
        options.addOption(start);
        options.addOption(end);
        options.addOption(fastq);
        options.addOption(tsv);
        options.addOption(counts);
        options.addOption(rna);
        options.addOption(or);
        options.addOption(threads);
        options.addOption(chunksize);
        options.addOption(kmerMap);
        options.addOption(snp);
        return options;
    }

    public String getOptionValue(String option){
        return cmd.getOptionValue(option);
    }

    public String getOptionValue(String option, String defaultValue){
        return cmd.getOptionValue(option, defaultValue);
    }

    public boolean hasOption(String option) {
        return cmd.hasOption(option);
    }

}
