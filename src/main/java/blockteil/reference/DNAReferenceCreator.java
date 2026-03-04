package blockteil.reference;

import blockteil.Config;
import blockteil.KMER;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class DNAReferenceCreator extends ReferenceKMERSetCreator {

    public DNAReferenceCreator(String fastaPath) {
        super(fastaPath);
    }

    public void addKMERS(Gene gene){
        Config.setGeneArray(new String[]{gene.gene_name}); // only one gene -> only one entry in geneRecord array
        byte[] referenceBases = getReferenceBases(gene.chromosome, gene.start, gene.end);
        addKMERS(referenceBases, (short) 0, 0, referenceBases.length);
    }

    public void addKMERS(Map<String, Gene> id2Gene) {
        Config.setGeneArray(getIdArray(id2Gene.keySet())); // either only gene list genes or all protein coding genes in id2gene
        Gene gene;
        for(short i = 0; i < Config.GENE_ARRAY.length; i++){
            gene = id2Gene.get(Config.GENE_ARRAY[i]);
            if(gene == null) continue;
            byte[] referenceBases = getReferenceBases(gene.chromosome, gene.start, gene.end);
            addKMERS(referenceBases, i, 0, referenceBases.length);
        }
    }

    public String[] getIdArray(Set<String> idSet) {
        return idSet.stream()
                .sorted()
                .toArray(String[]::new);
    }


}
