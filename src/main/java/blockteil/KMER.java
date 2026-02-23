package blockteil;

import java.util.Arrays;
import java.lang.StringBuilder;


public class KMER {
    public byte[] sequence;

    public KMER(byte[] sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        KMER other = (KMER) obj;
        if (sequence.length != other.sequence.length) return false;
        for (int i = 0; i < sequence.length; i++) {
            if (sequence[i] != other.sequence[i]) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(sequence);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (byte b : sequence) {
            sb.append((char) b);
        }
        return sb.toString();
    }
}
