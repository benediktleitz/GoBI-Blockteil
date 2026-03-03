package blockteil.reference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import java.util.Set;

public class DNAEinleseroutine extends Einleseroutine {
    private final String[] searchStrings = {"gene_id", "gene_biotype"};

    public DNAEinleseroutine(String filePath) {
        super(filePath);
    }

    public Map<String, Gene> read(Set<String> genesToAdd) {
        this.genesToAdd = genesToAdd;
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

                if(geneSkippable(attributes)) continue;

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
}
