package nxgen.messaging.client;

import nxgen.messaging.client.config.BrokerProperties;
import nxgen.messaging.client.event.serdes.EventDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class DefaultMessageQueueListener<M extends Message> implements MessageQueueListener<M>, Closeable
{
    private String topicName;
    private String groupId;
    private BrokerProperties brokerProperties;
    private MessageConsumer<M> eventConsumer;
    private ExecutorService executorService;
    private KafkaConsumer<String, M> kafkaConsumer;

    DefaultMessageQueueListener(String topicName, String groupId, BrokerProperties brokerProperties)
    {
        this.topicName = topicName;
        this.groupId = groupId;
        this.brokerProperties = brokerProperties;
    }

    @Override
    public void start(MessageConsumer<M> consumer)
    {
        eventConsumer = consumer;
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread newThread = new Thread(r);
            newThread.setName("event-queue-listener-thread");
            return newThread;
        });
        Properties properties = new Properties();
        properties.putAll(brokerProperties.toPropertiesMap());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getSimpleName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDeserializer.class.getSimpleName());
        kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(Collections.singleton(topicName));
        executorService.execute(this::poll);
    }

    private void poll()
    {
        boolean running = true;
        while (running) {
            try {
                eventConsumer.beforeConsume();
                ConsumerRecords<String, M> consumerRecords = kafkaConsumer.poll(200L);
                if (! consumerRecords.isEmpty()) {
                    Iterator<ConsumerRecord<String, M>> recordIterator = consumerRecords.records(topicName).iterator();
                    if (recordIterator.hasNext()) {
                        eventConsumer.consume(recordIterator.next().value());
                        kafkaConsumer.commitSync();
                    }
                }
                eventConsumer.afterPoll();
            }
            catch (Throwable throwable) {
                running = false;
            }
            if (running) {
                running = ! Thread.currentThread().isInterrupted();
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        executorService.shutdown();
        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }
    }
}