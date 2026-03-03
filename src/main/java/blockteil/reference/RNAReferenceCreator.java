package blockteil.reference;

import blockteil.Config;
import blockteil.KMER;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RNAReferenceCreator extends ReferenceKMERSetCreator{

    public RNAReferenceCreator(String fastaPath) {
        super(fastaPath);
    }

    public void addKMERS(Gene gene){
        System.err.println("Please don't provide location in RNA mode, we cant find transcripts with no provided gtf :(");
        System.err.println("(also we did not implement it, even if you do provide the gtf)");
        System.exit(1);
    }

    public void addKMERS(Map<String, Gene> id2Gene) {
        Config.setGeneArray(getIdArray(id2Gene)); // only relevant genes in id2gene
        byte[] referenceBases;
        List<Pair> exons;
        Pair exon;
        for(Gene gene : id2Gene.values()){
            referenceBases = getReferenceBases(gene.chromosome, gene.start, gene.end);
            for(RegionVector rv : gene.id2RegionVector.values()){
                exons = rv.get_sorted_pair_regions();
                exon = exons.getFirst();
                long kmer = addKMERS(referenceBases, rv.integerIndex, exon.start - gene.start, exon.end - gene.start + 1);
                for(int i = 1; i < exons.size(); i++){
                    exon = exons.get(i);
                    kmer = addKMERS(referenceBases, rv.integerIndex, exon.start - gene.start, exon.end - gene.start + 1, kmer);
                }
            }
        }
    }

    private String[] getIdArray(Map<String, Gene> id2gene) {
        List<RegionVector> transcripts = new ArrayList<>();
        for(Gene g : id2gene.values()){
            transcripts.addAll(g.id2RegionVector.values());
        }
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
