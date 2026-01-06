package chand.ankit.kv;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class LogSegment {
    private final File file;
    private final RandomAccessFile raf;

    public LogSegment(File file) throws IOException {
        this.file = file;
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        this.raf = new RandomAccessFile(file, "rw");
        this.raf.seek(this.raf.length());
    }

    public synchronized long append(String key, byte[] value) throws IOException {
        long pos = raf.getFilePointer();
        byte[] keyBytes = key.getBytes("UTF-8");
        raf.writeInt(keyBytes.length);
        raf.write(keyBytes);
        raf.writeInt(value.length);
        raf.write(value);
        raf.getFD().sync();
        return pos;
    }

    public synchronized Entry readAt(long position) throws IOException {
        raf.seek(position);
        int keyLen = raf.readInt();
        byte[] keyBytes = new byte[keyLen];
        raf.readFully(keyBytes);
        int valLen = raf.readInt();
        byte[] val = new byte[valLen];
        raf.readFully(val);
        return new Entry(new String(keyBytes, "UTF-8"), val);
    }

    public File getFile() {
        return file;
    }

    public long length() throws IOException {
        return raf.length();
    }

    public void close() throws IOException {
        raf.close();
    }

    public static class Entry {
        public final String key;
        public final byte[] value;

        public Entry(String key, byte[] value) {
            this.key = key;
            this.value = value;
        }
    }
}
