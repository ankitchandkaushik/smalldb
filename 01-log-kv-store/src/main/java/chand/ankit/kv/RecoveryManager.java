package chand.ankit.kv;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;

public class RecoveryManager {
    public static void recover(File logFile, Map<String, Long> index) throws IOException {
        if (!logFile.exists()) return;
        try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
            long pos = 0L;
            long len = raf.length();
            while (pos < len) {
                raf.seek(pos);
                int keyLen = raf.readInt();
                byte[] keyBytes = new byte[keyLen];
                raf.readFully(keyBytes);
                int valLen = raf.readInt();
                // skip value bytes but still read to advance pointer
                raf.skipBytes(valLen);
                String key = new String(keyBytes, "UTF-8");
                index.put(key, pos);
                pos = raf.getFilePointer();
            }
        }
    }
}
