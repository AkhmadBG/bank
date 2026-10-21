package ru.practicum.notifications.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import ru.practicum.interaction.kafka.NotificationMessage;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private static final String DLT_SUFFIX = ".DLT";
    private static final long RETRY_INTERVAL_MS = 1000L;
    private static final long RETRY_COUNT = 3L;

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<Class<?>, Serializer<?>> delegates = new LinkedHashMap<>();
        delegates.put(byte[].class, new ByteArraySerializer());
        delegates.put(NotificationMessage.class, new JsonSerializer<NotificationMessage>());

        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.ACKS_CONFIG, "all");

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                props, new StringSerializer(), new DelegatingByTypeSerializer(delegates)));
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1));

        log.info("Kafka error handler configured: {} retries, dead letter topic suffix '{}'",
                RETRY_COUNT, DLT_SUFFIX);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, RETRY_COUNT));
    }

}