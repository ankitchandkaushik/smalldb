package chand.ankit.kv;

public interface Serializer<T> {
    byte[] serialize(T obj);
    T deserialize(byte[] bytes);
}
