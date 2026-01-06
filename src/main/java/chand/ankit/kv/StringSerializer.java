package chand.ankit.kv;

import java.nio.charset.StandardCharsets;

public class StringSerializer implements Serializer<String> {

    @Override
    public byte[] serialize(String obj) {
        if (obj == null) return null;
        return obj.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String deserialize(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
