package com.org.therapistService.Config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

import com.fasterxml.jackson.databind.JsonNode;

@Configuration
public class KafkaConsumerConfig {

    @Autowired
    private ConsumerFactory<String, JsonNode> consumerFactory;

    @Autowired
    private KafkaTemplate<String, JsonNode> kafkaTemplate;

    @Bean
    DefaultErrorHandler therapistErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                // partition -1 lets the producer pick: the auto-created .DLT topic
                // has fewer partitions than the 3-partition source topics, so
                // targeting record.partition() fails for partitions 1 and 2
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", -1));

        return new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 2L));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, JsonNode> kafkaListenerContainerFactory(
            DefaultErrorHandler therapistErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, JsonNode> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(therapistErrorHandler);
        return factory;
    }
}
