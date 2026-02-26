package blockteil.readprocessing;

import blockteil.Config;

import java.util.BitSet;

public class FastqRecord {
    public String fw_header, rw_header, fw, rw, plus, fw_quality, rw_quality;

    public BitSet matchedGenes; // To store which genes this read matches

    public FastqRecord(String fw_header, String rw_header, String fw, String rw, String fw_quality, String rw_quality) {
        this.fw_header = fw_header; this.rw_header = rw_header; this.fw = fw; this.rw = rw; this.fw_quality = fw_quality; this.rw_quality = rw_quality;
    }

    public void setMatchedGenes(BitSet matchedGenes) {
        this.matchedGenes = matchedGenes;
    }

    public String toTSV() {
        if (matchedGenes == null || matchedGenes.cardinality() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(fw_header);
        for (int i = 0; i < Config.GENE_ARRAY.length; i++) {
            sb.append("\t")
                .append(matchedGenes.get(i) ? "1" : "0"); // Mark matched genes as 1, others as 0
        }
        return sb.toString();
    }

    public String[] toFastq() {
        if (matchedGenes == null || matchedGenes.cardinality() == 0) {
            return new String[0];
        }
        return new String[] {
            String.join("\n", fw_header, fw, "+", fw_quality),
            String.join("\n", rw_header, rw, "+", rw_quality)
        };
    }
}