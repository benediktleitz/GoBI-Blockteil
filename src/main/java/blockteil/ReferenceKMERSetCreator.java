package blockteil;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashSet;
import java.util.stream.Stream;
import java.nio.file.Paths;

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

    public void addKMERS(Gene gene){
        addKMERS(gene.chromosome(), gene.start(), gene.end(), 0);
    }

    public void addKMERS(String geneFilePath, Map<String, Gene> id2Gene) {
        Main.GENE_ARRAY = readGeneIds(geneFilePath);
        Gene g;
        for(int i = 0; i < Main.GENE_ARRAY.length; i++){
            g = id2Gene.get(Main.GENE_ARRAY[i]);
            if(g == null) continue;
            addKMERS(g.chromosome(), g.start(), g.end(), i);
        }
    }

    public String[] readGeneIds(String filePath) {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            return lines
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .sorted()
                    .toArray(String[]::new);
        }
        catch (Exception e){
            System.err.println("Error reading gene id file");
            e.printStackTrace();
        }
        return null;
    }
}
