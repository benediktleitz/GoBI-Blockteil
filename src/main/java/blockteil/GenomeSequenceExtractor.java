package blockteil;

import java.io.RandomAccessFile;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GenomeSequenceExtractor {
    
    RandomAccessFile raf;
    Map<String, long[]> idx;

    public GenomeSequenceExtractor(File fasta, File idx) {
        try {
            raf = new RandomAccessFile(fasta, "r");
        }
        catch (FileNotFoundException e) {}
        this.idx = readIndex(idx);
    }

    public Map<String, long[]> readIndex(File idx) {
        Map<String, long[]> indexMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(idx))) {
            String line;
            String[] parts;
            while ((line = br.readLine()) != null) {
                parts = line.split("\t");
                indexMap.put(parts[0], new long[]{Long.parseLong(parts[1]), Long.parseLong(parts[2]), Long.parseLong(parts[3]), Long.parseLong(parts[4])});
            }
        } catch (IOException e) {}
        return indexMap;
    }

    public byte[] getSequence(String chr, int start, int end) { // Let end be inclusice ?
        long[] info = idx.get(chr);
        if (info == null) return null;

        int bytesPerLine = (int) info[2];        // number of bases per line
        int bytesPerLineWithNewline = (int) info[3]; // total bytes including newline
        long fileStartOffset = info[1];          // offset in file where sequence starts

        // Convert to zero-based coordinates
        int startZero = start - 1;
        int endZero = end - 1;

        // Compute start and end line numbers and offsets
        long startLine = startZero / bytesPerLine;
        int startOffsetInLine = startZero % bytesPerLine;

        long endLine = endZero / bytesPerLine;
        int endOffsetInLine = endZero % bytesPerLine;

        // File offsets
        long readStart = fileStartOffset + startLine * bytesPerLineWithNewline + startOffsetInLine;
        long readEnd = fileStartOffset + endLine * bytesPerLineWithNewline + endOffsetInLine;
        int readBytes = (int) (readEnd - readStart + 1);

        byte[] byteSequence = new byte[readBytes];
        int actuallyRead = -1;
        try {
            raf.seek(readStart);
            actuallyRead = raf.read(byteSequence);
        } catch (IOException e) {}
        if (actuallyRead < 0) return new byte[0];

        // remove newlines
        int j = 0;
        for (int i = 0; i < actuallyRead; i++) {
            byte b = byteSequence[i];
            if (b != '\n') {
                byteSequence[j++] = b;
            }
        }

        if (j < actuallyRead) {
            byte[] trimmed = new byte[j];
            System.arraycopy(byteSequence, 0, trimmed, 0, j);
            return trimmed;
        } else return byteSequence;
        
    }

}