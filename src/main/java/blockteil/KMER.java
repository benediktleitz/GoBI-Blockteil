package blockteil;

public class KMER {

    private static final long mask;
    private static final byte[] BASE_TO_BITS = new byte[128];

    public static void init() {
        mask = (1L << (2 * Main.KMER_LENGTH)) - 1;
        BASE_TO_BITS['A'] = 0b00;
        BASE_TO_BITS['C'] = 0b01;
        BASE_TO_BITS['G'] = 0b10;
        BASE_TO_BITS['T'] = 0b11;
    }

    public static long makeKMER(byte[] sequence, int start) {
        long kmer = 0;
        for (int i = 0; i < Main.KMER_LENGTH; i++) {
            kmer <<= 2;
            kmer |= BASE_TO_BITS[sequence[start + i]];
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
        kmer |= BASE_TO_BITS[nextChar];
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

    public static String decodeKmer(long encoded) {
        char[] bases = new char[Main.KMER_LENGTH];

        for (int i = 0; i < Main.KMER_LENGTH; i++) {
            int shift = 2 * (Main.KMER_LENGTH - 1 - i);
            int bits = (int) ((encoded >> shift) & 0b11);

            bases[i] = switch (bits) {
                case 0 -> 'A';
                case 1 -> 'C';
                case 2 -> 'G';
                case 3 -> 'T';
                default -> throw new IllegalStateException();
            };
        }

        return new String(bases);
    }
}
