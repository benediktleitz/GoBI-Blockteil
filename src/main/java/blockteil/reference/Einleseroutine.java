package blockteil.reference;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class Einleseroutine {
    public Map<String, Gene> id2gene;
    public String filePath;
    public Set<String> genesToAdd;

    public Einleseroutine(String filePath){
        this.filePath = filePath;
        this.id2gene = new HashMap<>(30000);
    }

    public abstract Map<String, Gene> read(Set<String> genesToAdd);

    public boolean geneSkippable(Map<String, String> attributes){
        if(genesToAdd == null) { // Add all protein coding Genes
            return attributes.get("gene_biotype") == null || !attributes.get("gene_biotype").equals("protein_coding");
        }
        else{ // add all gene list genes
            return attributes.get("gene_id") == null || !genesToAdd.contains(attributes.get("gene_id"));
        }
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

    public String[] splitTabs(String line, String[] result) {
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
