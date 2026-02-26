package blockteil.reference;

import blockteil.Config;
import blockteil.KMER;

import java.io.File;
import java.lang.Runtime;


import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class ReferenceKMERSetCreator {

    protected final ReferenceSequenceFile fastaSequenceFile;

    public ReferenceKMERSetCreator(String fastaPath){
        this.fastaSequenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(fastaPath));
    }

    public byte[] getReferenceBases(String chr, int start, int end){
        ReferenceSequence referenceSequence = this.fastaSequenceFile.getSubsequenceAt(chr, start, end); // end inclusive, 1 based
        return referenceSequence.getBases();
    }

    protected void addKMERS(byte[] referenceBases, int idx, int regionStart, int regionEnd){
        long kmer = KMER.makeKMER(referenceBases, regionStart);
        Config.KMER_MAP
                .computeIfAbsent(kmer, k -> new IntOpenHashSet())
                .add(idx);
        for (int i = Config.KMER_LENGTH; i < regionEnd; i++) {
            kmer = KMER.shiftKMER(kmer, referenceBases[i]);
            Config.KMER_MAP
                    .computeIfAbsent(kmer, k -> new IntOpenHashSet())
                    .add(idx);
            if (Config.KMER_MAP.size() % 10000000 == 0) {
                System.out.println(Config.KMER_MAP.size() + " unique k-mers in map so far, at gene " + idx + "/" + Config.GENE_ARRAY.length);
                Runtime run = Runtime.getRuntime();
                System.out.println("Memory usage: " + (run.totalMemory() - run.freeMemory()) / (1024 * 1024) + " MB");
            }
        }
    }


}
