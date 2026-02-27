package blockteil.reference;

import blockteil.Config;
import blockteil.KMER;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RNAReferenceCreator extends ReferenceKMERSetCreator{
    private List<Gene> consideredGenes;

    public RNAReferenceCreator(String fastaPath) {
        super(fastaPath);
    }

    public void addAllTranscriptKMERS_DEPRECATED(Gene gene){
        byte[] referenceBases = getReferenceBases(gene.chromosome, gene.start, gene.end);
        for(RegionVector rv : gene.id2RegionVector.values()){
            for(Pair exon : rv.pair_regions){
                addKMERS(referenceBases, rv.integerIndex, exon.start - gene.start, exon.end - gene.start + 1); // incl start, excl end
            }
        }
    }

    public void addAllTranscriptKMERS(Gene gene){
        byte[] referenceBases = getReferenceBases(gene.chromosome, gene.start, gene.end);
        List<Pair> exons;
        Pair exon;
        for(RegionVector rv : gene.id2RegionVector.values()){
            exons = rv.get_sorted_pair_regions();
            exon = exons.getFirst();
            long kmer = addKMERS(referenceBases, rv.integerIndex, exon.start - gene.start, exon.end - gene.start + 1);
            for(int i = 1; i < exons.size(); i++){
                exon = exons.get(i);
                kmer = addKMERS(referenceBases, rv.integerIndex, exon.start - gene.start, exon.end - gene.start + 1, kmer);            }
        }
    }

    public void addKMERS(Gene gene){
        System.err.println("Please don't provide location in RNA mode, we cant find transcripts with no provided gtf :(");
        System.err.println("(also we did not implement it, even if you do provide the gtf)");
        System.exit(1);
    }

    public void addKMERS(String geneFilePath, Map<String, Gene> id2Gene) {
        if (geneFilePath == null) {
            Config.setGeneArray(getIdArray(id2Gene)); // no genes list -> all transcripts in GTF file
        } else {
            Config.setGeneArray(getIdArray(geneFilePath, id2Gene));
        }
        Collection<Gene> consideredGenes = this.consideredGenes == null ? id2Gene.values() : this.consideredGenes;
        for(Gene gene : consideredGenes){
            addAllTranscriptKMERS(gene);
        }
    }

    public String[] getIdArray(String filePath, Map<String, Gene> id2gene) {
        List<Gene> consideredGenes = new ArrayList<>();
        List<RegionVector> transcripts = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Gene g = id2gene.get(line);
                if (g == null) continue;
                transcripts.addAll(g.id2RegionVector.values());
                consideredGenes.add(g);
            }
            this.consideredGenes = consideredGenes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return assignedIndices(transcripts);
    }

    private String[] getIdArray(Map<String, Gene> id2gene) {
        List<RegionVector> transcripts = new ArrayList<>();
        for(Gene g : id2gene.values()){
            transcripts.addAll(g.id2RegionVector.values());
        }
        this.consideredGenes = null;
        return assignedIndices(transcripts);
    }

    public String[] assignedIndices(List<RegionVector> transcripts){
        transcripts.sort(Comparator.comparing(x -> x.transcript_id));
        String[] strings = new String[transcripts.size()];
        RegionVector rv;
        for(int i = 0; i < strings.length; i++){
            rv = transcripts.get(i);
            strings[i] = rv.transcript_id;
            rv.setIntegerIndex(i);
        }
        return strings;
    }
}
