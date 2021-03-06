package nxgen.kafka.client.event.serdes;

import nxgen.kafka.client.MessagingException;
import nxgen.kafka.client.event.Event;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.ExtendedDeserializer;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class EventDeserializer implements ExtendedDeserializer<Event>
{
    @Override
    public Event deserialize(String topic, Headers headers, byte[] data)
    {
        return deserialize(topic, data);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey)
    {
    }

    @Override
    public Event deserialize(String topic, byte[] data)
    {
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        DatumReader<nxgen.kafka.client.event.avro.Event> reader
                = new SpecificDatumReader<>(nxgen.kafka.client.event.avro.Event.getClassSchema());
        try {
            return toDomainEvent(reader.read(null, decoder));
        } catch (IOException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    public void close()
    {
    }

    private Event toDomainEvent(nxgen.kafka.client.event.avro.Event avroEvent)
    {
        Event domainEvent = new Event();
        domainEvent.setId(avroEvent.getId().toString());
        domainEvent.setType(avroEvent.getType().toString());
        domainEvent.setSender(avroEvent.getSender().array());
        domainEvent.setPayload(avroEvent.getPayload().array());
        domainEvent.setDateCreated(new Date(avroEvent.getDateCreated()));
        return domainEvent;
    }
}
