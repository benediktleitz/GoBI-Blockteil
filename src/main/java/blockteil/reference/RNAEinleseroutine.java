package blockteil.reference;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class RNAEinleseroutine extends Einleseroutine {
    private static final char SEP = '\t';
    private final String[] geneSearchStrings = {"gene_id", "gene_biotype"};
    private final String[] transcriptSearchStrings = {"transcript_id", "gene_id"};
    private final String filePath;


    public RNAEinleseroutine(String filePath) {
        this.filePath = filePath;
        this.id2gene = new HashMap<>(30000);

    }

    public Map<String, Gene> read(){
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
        if (attributes.get("gene_biotype") == null || !attributes.get("gene_biotype").equals("protein_coding")) return;
        String gene_id = attributes.get("gene_id");
        if (gene_id == null) return;
        Gene gene = get_gene(gene_id, tokens[0]);
        if (gene == null) return;
        gene.setStart(Integer.parseInt(tokens[3]));
        gene.setEnd(Integer.parseInt(tokens[4]));
    }

    public void parse_exon(String[] tokens, HashMap<String, String> attributes) {
        getAttributes(attributes, tokens[8], transcriptSearchStrings);
        String transcript_id = attributes.get("transcript_id");
        if(transcript_id == null) return;
        String gene_id = attributes.get("gene_id");
        if (gene_id == null) {
            System.out.println("No gene_id provided for Exon");
            return;
        }
        Gene gene = get_gene(gene_id, tokens[0]);
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

    // Parses the attribute column of a GTF/GFF3 file line
    // Attribute format example: gene_id "XYZ"; gene_name "ABC"; ...
    // Returns a map of attribute keys and values for the specified keys
    public Map<String, String> getAttributes(Map<String, String> attributes, String annotation, String[] searchStrings) {
        int keyStart, valueStart, valueEnd = 0;
        for (String key : searchStrings) {
            keyStart = annotation.indexOf(key + " \"");
            if (keyStart != -1) {
                valueStart = keyStart + key.length() + 2;
                valueEnd = annotation.indexOf("\"", valueStart);
                attributes.put(key, annotation.substring(valueStart, valueEnd));
            }
        }
        return attributes;
    }

    private static String[] splitTabs(String line, String[] result) {
        int tabCount = 0, lastIndex = 0, index = 0;
        while (tabCount < 8) {
            index = line.indexOf('\t', lastIndex);
            result[tabCount++] = line.substring(lastIndex, index);
            lastIndex = index + 1;
        }
        result[8] = line.substring(lastIndex);
        return result;
    }

}
