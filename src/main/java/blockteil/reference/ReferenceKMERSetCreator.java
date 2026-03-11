package blockteil.reference;

import blockteil.Config;
import blockteil.KMER;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.Runtime;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;
import htsjdk.samtools.reference.ReferenceSequenceFileFactory;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public abstract class ReferenceKMERSetCreator {

    protected final ReferenceSequenceFile fastaSequenceFile;

    public ReferenceKMERSetCreator(String fastaPath){
        this.fastaSequenceFile = ReferenceSequenceFileFactory.getReferenceSequenceFile(new File(fastaPath));
    }

    public byte[] getReferenceBases(String chr, int start, int end){
        ReferenceSequence referenceSequence = this.fastaSequenceFile.getSubsequenceAt(chr, start, end); // end inclusive, 1 based
        return referenceSequence.getBases();
    }

    // add kmers starting with new kmer creation
    protected long addKMERS(byte[] referenceBases, short idx, int regionStart, int regionEnd){
        long kmer = KMER.makeKMER(referenceBases, regionStart);
        Config.KMER_MAP
                .computeIfAbsent(kmer, k -> new IntOpenHashSet())
                .add(idx);
        for (int i = Config.KMER_LENGTH + regionStart; i < regionEnd; i++) {
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
        return kmer;
    }

    // add kmers with no kmer creation only shifting
    protected long addKMERS(byte[] referenceBases, short idx, int regionStart, int regionEnd, long kmer){
        Config.KMER_MAP
                .computeIfAbsent(kmer, k -> new IntOpenHashSet())
                .add(idx);
        for (int i = regionStart; i < regionEnd; i++) {
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
        return kmer;
    }

    public abstract void addKMERS(Gene g);

    public abstract void addKMERS(Map<String, Gene> id2Gene);

    public Set<String> readGeneList(String geneFilePath){
        Set<String> genesToAdd = new HashSet<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(geneFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                genesToAdd.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return genesToAdd;
    }

    public void addKMERSFromSNPFile(String snpDirPath, Map<String, Gene> id2Gene) {
        for (int i = 0; i < Config.GENE_ARRAY.length; i++) {
            final int geneIndex = i;
            String snpFilePath = snpDirPath + "/" + Config.GENE_ARRAY[i] + ".tsv";
            try (Stream<String> lines = Files.lines(Paths.get(snpFilePath))) {
                lines.skip(1).forEach(line -> {
                    line = line.trim();
                    if (line.isEmpty()) return;
                    String[] parts = line.split("\t");
                    String kmerStr = parts[0];
                    if (kmerStr.length() != Config.KMER_LENGTH) {
                        System.out.println("Skipping invalid k-mer: " + kmerStr + " in file: " + snpFilePath);
                        return;
                    }
                    long kmer = KMER.makeKMER(kmerStr, 0);
                    Config.KMER_MAP
                            .computeIfAbsent(kmer, k -> new IntOpenHashSet())
                            .add(geneIndex);
                });
            } catch (IOException e) {
                System.out.println("File not found: " + snpFilePath);
            }
        }
    }
}
