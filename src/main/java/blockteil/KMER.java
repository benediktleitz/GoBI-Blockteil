package blockteil;

public class KMER {

    private static long mask;
    private static final byte[] BASE_TO_BITS = new byte[128];
    private static final byte[] BASE_TO_BITS_COMPLEMENT = new byte[128];

    public static void init() {
        mask = (1L << (2 * Config.KMER_LENGTH)) - 1;
        BASE_TO_BITS['A'] = 0b00;
        BASE_TO_BITS['C'] = 0b01;
        BASE_TO_BITS['G'] = 0b10;
        BASE_TO_BITS['T'] = 0b11;
        BASE_TO_BITS_COMPLEMENT['A'] = 0b11; 
        BASE_TO_BITS_COMPLEMENT['C'] = 0b10; 
        BASE_TO_BITS_COMPLEMENT['G'] = 0b01; 
        BASE_TO_BITS_COMPLEMENT['T'] = 0b00;
    }

    public static long makeKMER(byte[] sequence, int start) {
        long kmer = 0;
        for (int i = 0; i < Config.KMER_LENGTH; i++) {
            // if (sequence[start + i] == 78) { // 'N'
            //     return Long.MIN_VALUE;
            // }
            kmer <<= 2;
            kmer |= BASE_TO_BITS[sequence[start + i]];
        }
        return kmer & mask;
    }

    public static long makeKMER(String sequence, int start) {
        return makeKMER(sequence.getBytes(), start);
    }

    public static long makeKMER_revcomp(byte[] sequence, int start) {
        long kmer = 0;
        for(int i = Config.KMER_LENGTH - 1; i >= 0; i--) {
            // if (sequence[start + i] == 78) { // 'N'
            //     return Long.MIN_VALUE;
            // }
            kmer <<= 2;
            kmer |= BASE_TO_BITS_COMPLEMENT[sequence[start + i]];
        }
        return kmer & mask;
    }

    public static long makeKMER_revcomp(String sequence, int start) {
        return makeKMER_revcomp(sequence.getBytes(), start);
    }

    public static long shiftKMER(long kmer, char nextChar) {
        // if (nextChar == 'N') {
        //     return Long.MIN_VALUE;
        // }
        kmer <<= 2;
        kmer |= BASE_TO_BITS[nextChar];
        return kmer & mask;
    }

    public static long shiftKMER_revcomp(long kmer, char nextChar) {
        // if (nextChar == 'N') {
        //     return Long.MIN_VALUE;
        // }
        kmer >>>= 2;
        kmer |= ((long) BASE_TO_BITS_COMPLEMENT[nextChar]) << (2 * (Config.KMER_LENGTH - 1));
        return kmer & mask;
    }

    public static long shiftKMER(long kmer, byte nextByte) {
        return shiftKMER(kmer, (char) nextByte);
    }
    
    public static long shiftKMER_revcomp(long kmer, byte nextByte) {
        return shiftKMER_revcomp(kmer, (char) nextByte);
    }

    public static String decodeKmer(long encoded) {
        char[] bases = new char[Config.KMER_LENGTH];

        for (int i = 0; i < Config.KMER_LENGTH; i++) {
            int shift = 2 * (Config.KMER_LENGTH - 1 - i);
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
