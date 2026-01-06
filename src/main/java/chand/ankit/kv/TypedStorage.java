package chand.ankit.kv;

import java.io.IOException;

public class TypedStorage<T> {
    private final StorageEngine engine;
    private final Serializer<T> serializer;

    public TypedStorage(StorageEngine engine, Serializer<T> serializer) {
        this.engine = engine;
        this.serializer = serializer;
    }

    public void put(String key, T value) throws IOException {
        byte[] bytes = serializer.serialize(value);
        engine.put(key, bytes == null ? new byte[0] : bytes);
    }

    public T get(String key) throws IOException {
        byte[] bytes = engine.get(key);
        if (bytes == null || bytes.length == 0) return null;
        return serializer.deserialize(bytes);
    }
}
