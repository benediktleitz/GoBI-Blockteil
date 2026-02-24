package blockteil;

public class KMER {

    private static final long mask;

    static {
        mask = (1L << Main.KMER_LENGTH) - 1;
    }

    private static long getBitRepresentation(char c) {
        switch (c) {
            case 'A':
                return 0b00;
            case 'C':
                return 0b01;
            case 'G':
                return 0b10;
            case 'T':
                return 0b11;
            default:
                throw new IllegalArgumentException("Invalid character: " + c);
        }
    }

    public static long makeKMER(byte[] sequence, int start) {
        long kmer = 0;
        for (int i = 0; i < Main.KMER_LENGTH; i++) {
            kmer <<= 2;
            kmer |= getBitRepresentation((char) sequence[start + i]);
        }
        return kmer & mask;
    }

    public static long makeKMER(byte[] sequence) {
        return makeKMER(sequence, 0);
    }

    public static long makeKMER(String sequence, int start) {
        return makeKMER(sequence.getBytes(), start);
    }

    public static long makeKMER(String sequence) {
        return makeKMER(sequence.getBytes(), 0);
    }

    public static long shiftKMER(long kmer, char nextChar) {
        kmer <<= 2;
        kmer |= getBitRepresentation(nextChar);
        return kmer & mask;
    }

    public static long shiftKMER(long kmer, byte nextByte) {
        return shiftKMER(kmer, (char) nextByte);
    }

    public static long shiftKMER(long kmer, byte[] sequence, int index) {
        return shiftKMER(kmer, (char) sequence[index]);
    }

    public static long shiftKMER(long kmer, String sequence, int index) {
        return shiftKMER(kmer, sequence.charAt(index));
    }

    public static long shiftKMER(long kmer, byte[] sequence, int index, int length) {
        for (int i = 0; i < length; i++) {
            kmer = shiftKMER(kmer, sequence[index + i]);
        }        
        return kmer;
    }
}
