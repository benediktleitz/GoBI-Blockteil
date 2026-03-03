package blockteil.reference;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.Set;

public class RNAEinleseroutine extends Einleseroutine {
    private final String[] geneSearchStrings = {"gene_id", "gene_biotype"};
    private final String[] transcriptSearchStrings = {"transcript_id", "gene_id", "gene_biotype"};

    public RNAEinleseroutine(String filePath) {
        super(filePath);
    }

    public Map<String, Gene> read(Set<String> genesToAdd){
        this.genesToAdd = genesToAdd;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(filePath))))) {
            String line;
            String[] row = new String[9];
            HashMap<String, String> attributes = new HashMap<>(3);

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                splitTabs(line, row);
                attributes.clear();
                switch (row[2]) {
                    case "exon" -> parse_exon(row, attributes);
                    case "gene" -> parse_gene(row, attributes);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading GTF file: " + filePath);
            e.printStackTrace();
        }
        return id2gene;
    }

    public void parse_gene(String[] tokens, HashMap<String, String> attributes) {
        getAttributes(attributes, tokens[8], geneSearchStrings);
        if(geneSkippable(attributes)) return;
        String gene_id = attributes.get("gene_id");
        if (gene_id == null) return;
        Gene gene = get_gene(gene_id, tokens[0]);
        if (gene == null) return;
        gene.setStart(Integer.parseInt(tokens[3]));
        gene.setEnd(Integer.parseInt(tokens[4]));
    }

    public void parse_exon(String[] tokens, HashMap<String, String> attributes) {
        getAttributes(attributes, tokens[8], transcriptSearchStrings);
        if(geneSkippable(attributes)) return;
        String transcript_id = attributes.get("transcript_id");
        if(transcript_id == null) return;
        String gene_id = attributes.get("gene_id");
        Gene gene = get_gene(gene_id, tokens[0]);
        if(gene == null) return;
        Pair pair = new Pair(Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4]));
        gene.add_exon(transcript_id, pair);
    }


    public Gene get_gene(String gene_id, String chr){
        Gene gene = id2gene.getOrDefault(gene_id, null);
        if(gene == null){
            gene = new Gene(chr);
            id2gene.put(gene_id, gene);
        }
        return gene;
    }

}
