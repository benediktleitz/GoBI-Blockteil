package blockteil.reference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RegionVector {
    public List<Pair> pair_regions;
    public String transcript_id;
    private List<Pair> sorted_pairs;
    public int integerIndex;

    public RegionVector(String transcript_id) {
        this.pair_regions = new ArrayList<>();
        this.transcript_id = transcript_id;
        this.sorted_pairs = null;
    }

    public void add_exon(Pair pair) {
        pair_regions.add(pair);
    }

    public List<Pair> get_sorted_pair_regions() {
        if (sorted_pairs == null) {
            this.sorted_pairs = pair_regions.stream()
                    .sorted(Comparator.comparingInt(x -> x.start)).
                    collect(Collectors.toList());
        }
        return sorted_pairs;
    }

    public String toString() {
        return pair_regions.stream().map(Object::toString).collect(Collectors.joining("|"));
    }

    @Override
    public int hashCode() {
        int res = 0;
        for(Pair pair : pair_regions) {
            res += 31 * pair.hashCode();
        }
        return res;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RegionVector)) return false;
        RegionVector other = (RegionVector) obj;

        if(this.get_sorted_pair_regions().size() != other.get_sorted_pair_regions().size()) return false;
        return Objects.equals(this.get_sorted_pair_regions(), other.get_sorted_pair_regions());
    }


    public void setIntegerIndex(int index) {
        integerIndex = index;
    }
}
