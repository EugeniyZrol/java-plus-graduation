package ru.practicum.ewm.stats.kafka;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DatumWriter<T> writer = new SpecificDatumWriter<>((Class<T>) data.getClass());
            Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new SerializationException("Error serializing Avro message", e);
        }
    }
}