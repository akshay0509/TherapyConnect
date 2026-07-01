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
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", record.partition()));

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
