package blockteil.reference;

import java.util.HashMap;
import java.util.List;

public class Gene {
    public String gene_name;
    public String chromosome;
    public HashMap<String, RegionVector> id2RegionVector;
    public List<Pair> merged_rv;
    public int start;
    public int end;

    public Gene(String chromosome) {
        this.chromosome = chromosome;
        this.id2RegionVector = new HashMap<>();
        this.merged_rv = null;
    }

    public Gene(String name, String chromosome, int start, int end){
        this.gene_name = name;
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
    }

    public void add_exon(String transcript_id, Pair pair) {
        RegionVector rv = id2RegionVector.getOrDefault(transcript_id, null);
        if (rv == null) {
            rv = new RegionVector(transcript_id);
            id2RegionVector.put(transcript_id, rv);
        }
        rv.add_exon(pair);
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}
