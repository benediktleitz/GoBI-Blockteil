package blockteil;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;

public class ReferenceKMERSetCreator {

    private final ReferenceSequenceFile fastaSequenceFile;

    public ReferenceKMERSetCreator(String fastaPath){
        this.fastaSequenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(fastaPath));
    }

    public void addKMERS(String chr, int start, int end, Integer geneIdx){
        ReferenceSequence referenceSequence = this.fastaSequenceFile.getSubsequenceAt(chr, start, end); // end inclusive, 1 based
        byte[] referenceBases = referenceSequence.getBases();
        long kmer = KMER.makeKMER(referenceBases, 0);
        Main.KMER_MAP.computeIfAbsent(kmer, k -> new HashSet<>())
                .add(geneIdx);
        for (int i = Main.KMER_LENGTH; i < referenceBases.length; i++) {
            kmer = KMER.shiftKMER(kmer, referenceBases[i]);
            Main.KMER_MAP.computeIfAbsent(kmer, k -> new HashSet<>())
                    .add(geneIdx);
        }
    }





    
}
