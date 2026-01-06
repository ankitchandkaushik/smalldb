package chand.ankit.kv;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Demo {
    public static void main(String[] args) throws Exception {
        File dataDir = new File("data-week1");

        System.out.println("=== Start engine and recover existing data ===");
        StorageEngine engine = new StorageEngine(dataDir);
        engine.recover();

        // show any existing values
        byte[] beforeHello = engine.get("hello");
        System.out.println("hello before -> " + (beforeHello == null ? "<null>" : new String(beforeHello, StandardCharsets.UTF_8)));

        // --- byte[] API (core) ---
        System.out.println("\n=== byte[] put/get ===");
        engine.put("hello", "world".getBytes(StandardCharsets.UTF_8));
        byte[] vHello = engine.get("hello");
        System.out.println("hello -> " + (vHello == null ? "<null>" : new String(vHello, StandardCharsets.UTF_8)));

        // --- Typed API via Serializer ---
        System.out.println("\n=== TypedStorage<String> ===");
        TypedStorage<String> typed = new TypedStorage<>(engine, new StringSerializer());
        typed.put("foo", "bar");
        String vFoo = typed.get("foo");
        System.out.println("foo -> " + (vFoo == null ? "<null>" : vFoo));

        // --- Streaming API for large blobs ---
        System.out.println("\n=== Streaming put/get ===");
        final int total = 2 * 1024 * 1024; // 2MB
        InputStream bigIn = new InputStream() {
            private int sent = 0;
            @Override
            public int read() {
                if (sent >= total) return -1;
                sent++;
                return 'x';
            }
        };
        engine.putStream("big", bigIn);

        try (InputStream in = engine.getStream("big")) {
            long cnt = 0;
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) cnt += r;
            System.out.println("big size read = " + cnt + " bytes");
        }

        // Also test materializing the streamed value via get()
        byte[] bigBytes = engine.get("big");
        System.out.println("big materialized size = " + (bigBytes == null ? "<null>" : bigBytes.length + " bytes"));
        if (bigBytes != null) {
            int previewLen = Math.min(bigBytes.length, 200);
            String preview = new String(bigBytes, 0, previewLen, StandardCharsets.UTF_8);
            System.out.println("big preview: " + preview + (bigBytes.length > previewLen ? "...(truncated)" : ""));
        }

        engine.close();

        // --- Re-open and recover to verify persistence ---
        System.out.println("\n=== Re-open engine and recover ===");
        StorageEngine engine2 = new StorageEngine(dataDir);
        engine2.recover();
        byte[] helloAfter = engine2.get("hello");
        System.out.println("hello after recover -> " + (helloAfter == null ? "<null>" : new String(helloAfter, StandardCharsets.UTF_8)));
        String fooAfter = new TypedStorage<>(engine2, new StringSerializer()).get("foo");
        System.out.println("foo after recover -> " + (fooAfter == null ? "<null>" : fooAfter));
        try (InputStream in2 = engine2.getStream("big")) {
            long cnt = 0;
            byte[] buf = new byte[8192];
            int r;
            while ((r = in2.read(buf)) != -1) cnt += r;
            System.out.println("big after recover size = " + cnt + " bytes");
        }
        byte[] bigAfter = engine2.get("big");
        System.out.println("big after materialized size = " + (bigAfter == null ? "<null>" : bigAfter.length + " bytes"));
        if (bigAfter != null) {
            int previewLen2 = Math.min(bigAfter.length, 200);
            String preview2 = new String(bigAfter, 0, previewLen2, StandardCharsets.UTF_8);
            System.out.println("big after preview: " + preview2 + (bigAfter.length > previewLen2 ? "...(truncated)" : ""));
        }
        // --- Delete tests: delete keys and verify immediate and persisted deletion ---
        System.out.println("\n=== Delete tests ===");
        System.out.println("deleting foo and big...");
        engine2.delete("foo");
        engine2.delete("big");
        System.out.println("foo after delete -> " + (engine2.get("foo") == null ? "<null>" : "present"));
        System.out.println("big stream after delete -> " + (engine2.getStream("big") == null ? "<null>" : "present"));

        engine2.close();

        // Re-open to confirm deletes persisted
        System.out.println("\n=== Re-open engine to verify deletes persisted ===");
        StorageEngine engine3 = new StorageEngine(dataDir);
        engine3.recover();
        System.out.println("foo after reopen -> " + (engine3.get("foo") == null ? "<null>" : "present"));
        System.out.println("big after reopen (stream) -> " + (engine3.getStream("big") == null ? "<null>" : "present"));
        engine3.close();
    }
}
