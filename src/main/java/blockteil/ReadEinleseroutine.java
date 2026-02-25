package blockteil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.locks.ReentrantLock;

public class ReadEinleseroutine {

    private static final int CHUNK_SIZE = 50000; // number of reads per chunk

    // Class to hold one FASTQ record
    static class FastqRecord {
        String header, fw, rw, plus, fw_quality, rw_quality;

        BitSet matchedGenes; // To store which genes this read matches

        FastqRecord(String h, String fw, String rw, String fw_quality, String rw_quality) {
            this.header = h; this.fw = fw; this.rw = rw; this.fw_quality = fw_quality; this.rw_quality = rw_quality;
        }

        public void setMatchedGenes(BitSet matchedGenes) {
            this.matchedGenes = matchedGenes;
        }

        @Override
        public String toString() {
            if (matchedGenes == null || matchedGenes.cardinality() == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(header);
            for (int i = 0; i < matchedGenes.length(); i++) {
                sb.append("\t")
                    .append(matchedGenes.get(i) ? "1" : "0"); // Mark matched genes as 1, others as 0
            }
            return sb.toString();
        }
    }

    public static void filterReads(String fw_file, String rw_file, Path outputFile) {
        int numThreads = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        ReentrantLock writeLock = new ReentrantLock();  // ensures safe writes
        List<Future<?>> futures = new ArrayList<>();

        try {
            if (outputFile.getParent() != null) {
                Files.createDirectories(outputFile.getParent());
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create output directory for " + outputFile, e);
        }

        try (BufferedReader br_fw = new BufferedReader(
                 new InputStreamReader(new GZIPInputStream(new FileInputStream(fw_file))));
             BufferedReader br_rw = new BufferedReader(
                 new InputStreamReader(new GZIPInputStream(new FileInputStream(rw_file))));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.toFile()))) {

            List<FastqRecord> chunk = new ArrayList<>(CHUNK_SIZE);
            String line_fw;
            String line_rw;

            writeLock.lock();
            bw.write("read_id\t");
            bw.write(String.join("\t", Main.GENE_ARRAY));
            bw.newLine();
            writeLock.unlock();

            while ((line_fw = br_fw.readLine()) != null && (line_rw = br_rw.readLine()) != null) { // Ensure both files have the same number of lines
                String header = line_fw;
                String fw = br_fw.readLine();
                br_fw.readLine();
                String fw_quality = br_fw.readLine();

                String rw = br_rw.readLine();
                br_rw.readLine();
                String rw_quality = br_rw.readLine();

                chunk.add(new FastqRecord(header, fw, rw, fw_quality, rw_quality));

                if (chunk.size() >= CHUNK_SIZE) {
                    // Submit chunk for parallel processing
                    List<FastqRecord> chunkToProcess = new ArrayList<>(chunk);
                    futures.add(executor.submit(() -> {
                        List<FastqRecord> processed = processChunk(chunkToProcess);
                        writeChunk(processed, bw, writeLock);
                    }));
                    chunk.clear();
                }
            }

            // Submit remaining records
            if (!chunk.isEmpty()) {
                List<FastqRecord> chunkToProcess = new ArrayList<>(chunk);
                futures.add(executor.submit(() -> {
                    List<FastqRecord> processed = processChunk(chunkToProcess);
                    writeChunk(processed, bw, writeLock);
                }));
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Filtering was interrupted", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("Worker failed during chunk processing", e.getCause());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }        
    }

    private static List<FastqRecord> processChunk(List<FastqRecord> chunk) {
        for (FastqRecord r : chunk) {
            r.setMatchedGenes(KMERFilterer.filterKMER(r.fw, r.rw));
        }
        return chunk;
    }

    // Thread-safe writing of processed chunk
    private static void writeChunk(List<FastqRecord> chunk, BufferedWriter bw, ReentrantLock lock) {
        lock.lock();
        try {
            for (FastqRecord r : chunk) {
                String line = r.toString();
                if (line.isEmpty()) {
                    continue;
                }
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}