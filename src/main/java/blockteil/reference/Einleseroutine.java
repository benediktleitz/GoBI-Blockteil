package blockteil.reference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

public class Einleseroutine {
    public String filePath;
    public Map<String, Gene> id2gene;
    private final String[] searchStrings = {"gene_id", "gene_biotype"};

    public Einleseroutine(String filePath) {
        this.filePath = filePath;
        this.id2gene = new HashMap<>(30000);
    }

    public Map<String, Gene> read() {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(filePath))))) {
            String line;
            Map<String, String> attributes = new HashMap<>(5);
            String[] row = new String[9];
            String gene_id;
            Gene gene;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                splitTabs(line, row);
                attributes.clear();
                if (row[2].equals("gene"))
                    getAttributes(attributes, row[8], searchStrings);
                else continue;
                if (attributes.get("gene_biotype") == null || !attributes.get("gene_biotype").equals("protein_coding")) continue;

                gene_id = attributes.get("gene_id");
                int start = Integer.parseInt(row[3]);
                int end = Integer.parseInt(row[4]);

                gene = new Gene(
                    gene_id,
                    row[0],
                    start,
                    end
                );
                id2gene.put(gene_id, gene);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id2gene;
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
