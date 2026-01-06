package chand.ankit.kv;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RecoveryManager {
    // sensible sanity limits to avoid OOM or bogus lengths
    private static final int MAX_KEY_BYTES = 10_000_000; // 10 MB
    private static final int MAX_CHUNK_BYTES = 16 * 1024 * 1024; // 16 MB

    public static void recover(File logFile, Map<String, Long> index) throws IOException {
        if (!logFile.exists()) return;
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long pos = 0L;
            long len = raf.length();
            long lastGood = 0L;
            while (pos < len) {
                raf.seek(pos);
                try {
                    int keyLen = raf.readInt();
                    if (keyLen < 0 || keyLen > MAX_KEY_BYTES) {
                        // malformed or unexpected; stop recovery
                        break;
                    }
                    byte[] keyBytes = new byte[keyLen];
                    raf.readFully(keyBytes);

                    int valLen = raf.readInt();
                    if (valLen >= 0) {
                        long next = raf.getFilePointer() + (long) valLen;
                        if (next > raf.length()) throw new EOFException();
                        raf.seek(next);
                    } else if (valLen == -1) {
                        // chunked format: repeated (chunkLen, chunkBytes...), terminator chunkLen==0
                        while (true) {
                            int chunkLen = raf.readInt();
                            if (chunkLen < 0 || chunkLen > MAX_CHUNK_BYTES) {
                                // malformed
                                throw new IOException("invalid chunk length: " + chunkLen);
                            }
                            if (chunkLen == 0) break;
                            long next = raf.getFilePointer() + (long) chunkLen;
                            if (next > raf.length()) throw new EOFException();
                            raf.seek(next);
                        }
                    } else if (valLen == -2) {
                        // tombstone: remove key from index
                        index.remove(new String(keyBytes, StandardCharsets.UTF_8));
                        lastGood = pos;
                        pos = raf.getFilePointer();
                        continue;
                    } else {
                        // unsupported valLen marker
                        throw new IOException("unsupported valLen marker: " + valLen);
                    }
                    String key = new String(keyBytes, StandardCharsets.UTF_8);
                    index.put(key, pos);
                    lastGood = pos;
                    pos = raf.getFilePointer();
                } catch (EOFException  e) {
                    // partial or corrupted record detected — stop recovery at lastGood
                    break;
                }
                catch ( IOException e) {
                    // partial or corrupted record detected — stop recovery at lastGood
                    break;
                }
            }
        }
    }
}
