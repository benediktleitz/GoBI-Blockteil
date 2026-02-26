package blockteil.reference;

public class Pair {
    public int start;
    public int end;

    public Pair(int start, int end) {
        this.start = Math.min(start, end);
        this.end = Math.max(start, end);
    }

    public String toString() {
        return start + ":" + (end + 1);
        // [start, end)
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair) {
            Pair p = (Pair) obj;
            return start == p.start && end == p.end;
        }
        return false;
    }

    public int get_length(){
        return end - start + 1;
        // end - start always positive bc max, min
    }

    @Override
    public int hashCode() {
        return 31 * start + end;
    }


}
