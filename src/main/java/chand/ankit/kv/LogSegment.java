package chand.ankit.kv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

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
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        raf.writeInt(keyBytes.length);
        raf.write(keyBytes);
        raf.writeInt(value.length);
        raf.write(value);
        raf.getFD().sync();
        return pos;
    }

    /**
     * Append a large value using a chunked format.
     * Format: keyLen, key, valLen = -1, [chunkLen, chunkBytes]*, chunkLen=0 terminator
     */
    public synchronized long appendStream(String key, InputStream in) throws IOException {
        long pos = raf.getFilePointer();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        raf.writeInt(keyBytes.length);
        raf.write(keyBytes);
        raf.writeInt(-1); // indicate chunked stream

        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            raf.writeInt(r);
            raf.write(buf, 0, r);
        }
        raf.writeInt(0); // terminator
        raf.getFD().sync();
        return pos;
    }

    /**
     * Append a tombstone (delete) record for the given key.
     * Format: keyLen, key, valLen = -2
     */
    public synchronized long appendDelete(String key) throws IOException {
        long pos = raf.getFilePointer();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        raf.writeInt(keyBytes.length);
        raf.write(keyBytes);
        raf.writeInt(-2); // tombstone marker
        raf.getFD().sync();
        return pos;
    }

    public synchronized Entry readAt(long position) throws IOException {
        raf.seek(position);
        int keyLen = raf.readInt();
        byte[] keyBytes = new byte[keyLen];
        raf.readFully(keyBytes);
        int valLen = raf.readInt();
        if (valLen >= 0) {
            byte[] val = new byte[valLen];
            raf.readFully(val);
            return new Entry(new String(keyBytes, StandardCharsets.UTF_8), val);
        } else if (valLen == -1) {
            // chunked — read into memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int chunkLen = raf.readInt();
                if (chunkLen == 0) break;
                byte[] c = new byte[chunkLen];
                raf.readFully(c);
                baos.write(c);
            }
            return new Entry(new String(keyBytes, StandardCharsets.UTF_8), baos.toByteArray());
        } else if (valLen == -2) {
            // tombstone
            return new Entry(new String(keyBytes, StandardCharsets.UTF_8), null);
        } else {
            throw new IOException("unsupported valLen marker: " + valLen);
        }
    }

    /**
     * Return an InputStream that reads the value (fixed or chunked) starting at the record position.
     * Caller must close the stream.
     */
    public InputStream readStreamAt(long position) throws IOException {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        r.seek(position);
        int keyLen = r.readInt();
        r.skipBytes(keyLen);
        int valLen = r.readInt();
        if (valLen >= 0) {
            final long start = r.getFilePointer();
            return new RandomAccessFileInputStream(r, valLen, start);
        } else if (valLen == -1) {
            final long chunkStart = r.getFilePointer();
            return new ChunkedRandomAccessFileInputStream(r, chunkStart);
        } else if (valLen == -2) {
            r.close();
            return null; // tombstone
        } else {
            r.close();
            throw new IOException("unsupported valLen marker: " + valLen);
        }
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

    private static class RandomAccessFileInputStream extends InputStream {
        private final RandomAccessFile raf;
        private long remaining;

        RandomAccessFileInputStream(RandomAccessFile raf, long len, long startPos) throws IOException {
            this.raf = raf;
            this.remaining = len;
            raf.seek(startPos);
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = raf.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int r = raf.read(b, off, toRead);
            if (r > 0) remaining -= r;
            return r;
        }

        @Override
        public void close() throws IOException {
            raf.close();
            super.close();
        }
    }

    private static class ChunkedRandomAccessFileInputStream extends InputStream {
        private final RandomAccessFile raf;
        private int chunkRemaining = 0;
        private boolean finished = false;

        ChunkedRandomAccessFileInputStream(RandomAccessFile raf, long startPos) throws IOException {
            this.raf = raf;
            raf.seek(startPos);
            advanceChunk();
        }

        private void advanceChunk() throws IOException {
            int chunkLen = raf.readInt();
            if (chunkLen == 0) {
                finished = true;
                chunkRemaining = 0;
            } else {
                chunkRemaining = chunkLen;
            }
        }

        @Override
        public int read() throws IOException {
            if (finished) return -1;
            if (chunkRemaining == 0) {
                advanceChunk();
                if (finished) return -1;
            }
            int b = raf.read();
            if (b != -1) chunkRemaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (finished) return -1;
            if (chunkRemaining == 0) {
                advanceChunk();
                if (finished) return -1;
            }
            int toRead = Math.min(len, chunkRemaining);
            int r = raf.read(b, off, toRead);
            if (r > 0) chunkRemaining -= r;
            return r;
        }

        @Override
        public void close() throws IOException {
            raf.close();
            super.close();
        }
    }
}
