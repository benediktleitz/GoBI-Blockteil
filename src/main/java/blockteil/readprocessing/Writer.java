package blockteil.readprocessing;

import blockteil.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Writer {
    public static BufferedWriter[] makeBufferedWriters(String outputDir) throws IOException {
        BufferedWriter[] writers = new BufferedWriter[3];
        writers[0] = Config.WRITE_TSV ? new BufferedWriter(new FileWriter(outputDir + "/read2gene_matrix.tsv")) : null;
        writers[1] = Config.WRITE_FASTQ ? new BufferedWriter(new FileWriter(outputDir + "/fw.fastq")) : null;
        writers[2] = Config.WRITE_FASTQ ? new BufferedWriter(new FileWriter(outputDir + "/rw.fastq")) : null;
        return writers;
    }

    public static void writeTSVHeader(BufferedWriter bw, ReentrantLock lock) throws IOException {
        lock.lock();
        try {
            bw.write("read_id\t");
            bw.write(String.join("\t", Config.GENE_ARRAY));
            bw.newLine();
        } finally {
            lock.unlock();
        }
    }

    // Thread-safe writing of processed chunk
    public static void writeChunk(List<FastqRecord> chunk, BufferedWriter[] writers, ReentrantLock lock) {
        lock.lock();
        try {
            writeTSVChunk(chunk, writers[0]);
            writeFastqChunk(chunk, writers[1], writers[2]);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void writeTSVChunk(List<FastqRecord> chunk, BufferedWriter bw) throws IOException {
        if (bw == null) return;
        for (FastqRecord r : chunk) {
            String line = r.toTSV();
            if (line.isEmpty()) {
                continue;
            }
            bw.write(line);
            bw.newLine();
        }
    }

    public static void writeFastqChunk(List<FastqRecord> chunk, BufferedWriter fwWriter, BufferedWriter rwWriter) throws IOException {
        if (fwWriter == null || rwWriter == null) return;
        for (FastqRecord r : chunk) {
            String[] fastqLines = r.toFastq();
            if (fastqLines.length == 0) {
                continue;
            }
            fwWriter.write(fastqLines[0]);
            fwWriter.newLine();
            rwWriter.write(fastqLines[1]);
            rwWriter.newLine();
        }
    }

    public static void writeGeneCounts(Path outputFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            bw.write("gene\tcount\n");
            for (int i = 0; i < Config.GENE_ARRAY.length; i++) {
                bw.write(Config.GENE_ARRAY[i] + "\t" + Config.COUNT_ARRAY[i] + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
