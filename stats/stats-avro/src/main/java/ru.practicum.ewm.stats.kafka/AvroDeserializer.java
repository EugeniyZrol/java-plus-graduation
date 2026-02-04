package ru.practicum.ewm.stats.kafka;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final Class<T> targetType;

    public AvroDeserializer(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            DatumReader<T> reader = new SpecificDatumReader<>(targetType);
            Decoder decoder = DecoderFactory.get().binaryDecoder(in, null);
            return reader.read(null, decoder);
        } catch (IOException | RuntimeException e) {
            throw new SerializationException("Error deserializing Avro message", e);
        }
    }
}