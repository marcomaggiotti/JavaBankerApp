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

    public static <T> Serializer<T> serializer() {
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
