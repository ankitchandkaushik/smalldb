package chand.ankit.kv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class StorageEngine {
    private final LogSegment segment;
    private final Map<String, Long> index = new HashMap<>();

    public StorageEngine(File dir) throws IOException {
        File logFile = new File(dir, "log.dat");
        this.segment = new LogSegment(logFile);
    }

    public synchronized void put(String key, byte[] value) throws IOException {
        long pos = segment.append(key, value);
        index.put(key, pos);
    }

    public synchronized void putStream(String key, InputStream in) throws IOException {
        long pos = segment.appendStream(key, in);
        index.put(key, pos);
    }

    public synchronized void delete(String key) throws IOException {
        long pos = segment.appendDelete(key);
        // reflect deletion in in-memory index
        index.remove(key);
    }

    public synchronized byte[] get(String key) throws IOException {
        Long pos = index.get(key);
        if (pos == null) return null;
        LogSegment.Entry e = segment.readAt(pos);
        return e.value;
    }

    public synchronized InputStream getStream(String key) throws IOException {
        Long pos = index.get(key);
        if (pos == null) return null;
        return segment.readStreamAt(pos);
    }

    public Map<String, Long> getIndexSnapshot() {
        return new HashMap<>(index);
    }

    public void recover() throws IOException {
        RecoveryManager.recover(segment.getFile(), index);
    }

    public void close() throws IOException {
        segment.close();
    }
}
