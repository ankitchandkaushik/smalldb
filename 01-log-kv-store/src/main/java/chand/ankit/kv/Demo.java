package chand.ankit.kv;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class Demo {
    public static void main(String[] args) throws Exception {
        File dataDir = new File("data-week1");
        StorageEngine engine = new StorageEngine(dataDir);

        // If there is an existing log, rebuild in-memory index
        engine.recover();

        // Put a few keys
        engine.put("hello", "world".getBytes(StandardCharsets.UTF_8));
        engine.put("foo", "bar".getBytes(StandardCharsets.UTF_8));

        // Read back
        byte[] v1 = engine.get("hello");
        System.out.println("hello -> " + (v1 == null ? "<null>" : new String(v1, StandardCharsets.UTF_8)));

        byte[] v2 = engine.get("foo");
        System.out.println("foo -> " + (v2 == null ? "<null>" : new String(v2, StandardCharsets.UTF_8)));

        engine.close();
    }
}
