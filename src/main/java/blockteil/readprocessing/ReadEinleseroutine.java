package blockteil.readprocessing;

import blockteil.Config;

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


    public static void filterReads(String fw_file, String rw_file, String outputDir) {
        int numThreads = Runtime.getRuntime().availableProcessors();

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        ReentrantLock writeLock = new ReentrantLock();  // ensures safe writes
        List<Future<?>> futures = new ArrayList<>();
        BufferedWriter[] writers = null;

        try (BufferedReader br_fw = new BufferedReader(
                 new InputStreamReader(new GZIPInputStream(new FileInputStream(fw_file))));
             BufferedReader br_rw = new BufferedReader(
                 new InputStreamReader(new GZIPInputStream(new FileInputStream(rw_file))));
            ) {
            writers = Writer.makeBufferedWriters(outputDir);
            final BufferedWriter[] sharedWriters = writers;
            List<FastqRecord> chunk = new ArrayList<>(CHUNK_SIZE);
            String line_fw;
            String line_rw;

            if (Config.WRITE_TSV) {
                Writer.writeTSVHeader(writers[0], writeLock);
            }

            while ((line_fw = br_fw.readLine()) != null && (line_rw = br_rw.readLine()) != null) { // Ensure both files have the same number of lines
                String fw = br_fw.readLine();
                br_fw.readLine();
                String fw_quality = br_fw.readLine();

                String rw = br_rw.readLine();
                br_rw.readLine();
                String rw_quality = br_rw.readLine();

                chunk.add(new FastqRecord(line_fw, line_rw, fw, rw, fw_quality, rw_quality));

                if (chunk.size() >= CHUNK_SIZE) {
                    // Submit chunk for parallel processing
                    List<FastqRecord> chunkToProcess = new ArrayList<>(chunk);
                    futures.add(executor.submit(() -> {
                        List<FastqRecord> processed = processChunk(chunkToProcess);
                        Writer.writeChunk(processed, sharedWriters, writeLock);
                    }));
                    chunk.clear();
                }
            }

            // Submit remaining records
            if (!chunk.isEmpty()) {
                List<FastqRecord> chunkToProcess = new ArrayList<>(chunk);
                futures.add(executor.submit(() -> {
                    List<FastqRecord> processed = processChunk(chunkToProcess);
                    Writer.writeChunk(processed, sharedWriters, writeLock);
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
            if (writers != null) {
                for (BufferedWriter writer : writers) {
                    if (writer == null) continue;
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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
            BitSet matchedGenes = Config.KMER_FILTERER.filterKMER(r.fw, r.rw);
            r.setMatchedGenes(matchedGenes);
        }
        return chunk;
    }
}