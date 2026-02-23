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
        Option fastq = Option.builder("fastq")
                .hasArg()
                .desc("path to fastq file")
                .required()
                .build();

        Option frlength = Option.builder("frlength")
                .hasArg()
                .desc("fragment length distribution: mean")
                .required()
                .build();

        Option sd = Option.builder("SD")
                .hasArg()
                .desc("Fragment length distribution: Standard Deviation")
                .required()
                .build();

        Option readcounts = Option.builder("readcounts")
                .hasArg()
                .desc("table of gene_id, transcript_id, count tuples")
                .required()
                .build();

        Option mutationrate = Option.builder("mutationrate")
                .hasArg()
                .desc("mutation rate in percent: proportion of simulated bases to be muatated")
                .required()
                .build();

        Option fasta = Option.builder("fasta")
                .hasArg()
                .desc("genome FASTA file")
                .required()
                .build();

        Option fidx = Option.builder("fidx")
                .hasArg()
                .desc("genome FASTA file index")
                .required()
                .build();

        Option gtf = Option.builder("gtf")
                .hasArg()
                .desc("GTF File")
                .required()
                .build();

        Option output_dir = Option.builder("od")
                .hasArg()
                .desc("output directory")
                .required()
                .build();

        options.addOption(fastq);
        options.addOption(frlength);
        options.addOption(sd);
        options.addOption(readcounts);
        options.addOption(mutationrate);
        options.addOption(fasta);
        options.addOption(fidx);
        options.addOption(gtf);
        options.addOption(output_dir);
        return options;
    }

    public String getOptionValue(String option){
        return cmd.getOptionValue(option);
    }

}
