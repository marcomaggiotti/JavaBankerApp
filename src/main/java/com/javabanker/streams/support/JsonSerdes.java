package com.javabanker.streams.support;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

/**
 * Minimal JSON Serde factory. Explicit on purpose: no type headers, no magic, the wire format is the
 * record's JSON. In production you would often use Avro/Protobuf + Schema Registry instead
 * (talk about schema evolution in the interview!).
 */
public final class JsonSerdes {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // forward compatibility

    private JsonSerdes() {
    }

    public static <T> Serde<T> of(Class<T> type) {
        return Serdes.serdeFrom(serializer(), deserializer(type));
    }

    /**
     * Returns a Kafka {@link Serializer} written as a <b>lambda</b>.
     * <p>
     * This works because {@code Serializer} is a <i>functional interface</i> in practice: it has exactly one
     * abstract method, {@code byte[] serialize(String topic, T data)} ({@code configure} and {@code close}
     * have default implementations). Any interface with a single abstract method can be implemented
     * with a lambda. The equivalent pre-Java-8 anonymous class is:
     * <pre>{@code
     * return new Serializer<T>() {
     *     @Override
     *     public byte[] serialize(String topic, T data) { ... }
     * };
     * }</pre>
     * A multi-line lambda uses braces and an explicit {@code return}, like a method body.
     */
    public static <T> Serializer<T> serializer() {
        // (topic, data): the parameters of Serializer.serialize; their types come from the return type.
        return (topic, data) -> {
            if (data == null) {
                return null;
            }
            try {
                return MAPPER.writeValueAsBytes(data);
            } catch (IOException e) {
                throw new SerializationException("Cannot serialize value for topic " + topic, e);
            }
        };
    }

    /**
     * Same idea as {@link #serializer()}: {@code Deserializer.deserialize(String, byte[])} implemented as a
     * lambda. The lambda uses the parameter {@code type} of the enclosing method; this is allowed because
     * {@code type} is <i>effectively final</i> (never reassigned). Each call to this method returns a new
     * lambda that has captured its own {@code type}.
     */
    public static <T> Deserializer<T> deserializer(Class<T> type) {
        return (topic, bytes) -> {
            if (bytes == null) {
                return null;
            }
            try {
                return MAPPER.readValue(bytes, type);
            } catch (IOException e) {
                throw new SerializationException("Cannot deserialize " + type.getSimpleName() + " from " + topic, e);
            }
        };
    }
}
