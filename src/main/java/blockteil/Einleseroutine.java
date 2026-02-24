package blockteil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.HashMap;

public class Einleseroutine {
    public String filePath;
    public Map<String, Gene> genes;
    private final String[] searchStringsTranscript = {"gene_id", "transcript_id"};
    private final String[] searchStrings = {"gene_id"};

    public Einleseroutine(String filePath) {
        this.filePath = filePath;
        this.genes = new HashMap<>(30000);
    }

    public Map<String, Gene> read() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            Map<String, String> attributes = new HashMap<>(5);
            String[] row = new String[9];
            String gene_id;
            Gene gene;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                splitTabs(line, row);
                attributes.clear();
                if (row[2].equals("CDS") || row[2].equals("exon"))
                    getAttributes(attributes, row[8], searchStringsTranscript);
                else
                    getAttributes(attributes, row[8], searchStrings);

                gene_id = attributes.get("gene_id");
                gene = genes.get(gene_id);
                if (gene == null) {
                    // Create new Gene object and add to genes map
                    gene = new Gene(
                        gene_id,
                        row[0],
                        row[6].charAt(0)
                    );
                    genes.put(gene_id, gene);
                }    
                
                int start = Integer.parseInt(row[3]);
                gene.addStart(start);
                int end = Integer.parseInt(row[4]);
                gene.addEnd(end);
                if (row[2].equals("CDS"))
                    gene.addTranscript(start, end, attributes.get("transcript_id"), false);
                else if (row[2].equals("exon"))
                    gene.addTranscript(start, end, attributes.get("transcript_id"), true);
            }
        } catch (Exception e) {}
        return genes;
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

    private String[] splitTabs(String line, String[] result) {
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
