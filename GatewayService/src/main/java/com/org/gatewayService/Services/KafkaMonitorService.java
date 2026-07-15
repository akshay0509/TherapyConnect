package com.org.gatewayService.Services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

/**
 * Read-side monitoring of the Kafka estate for the admin dashboard: DLT
 * depth, consumer-group lag, and DLT peek/replay.
 *
 * "Pending" DLT messages are the ones not yet replayed: depth is measured
 * against the committed offsets of the {@value #REPLAY_GROUP} consumer group,
 * which only ever advances when an admin triggers a replay. Kafka topics are
 * append-only, so replayed messages stay in the DLT until retention expires —
 * the committed offset is what separates handled from unhandled.
 */
@Service
public class KafkaMonitorService {

    private static final String REPLAY_GROUP = "dlt-replay";
    private static final String PEEK_GROUP = "dlt-peek";
    private static final long API_TIMEOUT_SECONDS = 5;
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);
    private static final int MAX_PEEK = 50;
    private static final int MAX_PAYLOAD_CHARS = 4000;
    private static final long REPLAY_DEADLINE_MS = 30_000;

    private static final Logger logger = LoggerFactory.getLogger(KafkaMonitorService.class);

    private final KafkaAdmin kafkaAdmin;
    private final String bootstrapServers;

    public KafkaMonitorService(KafkaAdmin kafkaAdmin,
                               @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        this.kafkaAdmin = kafkaAdmin;
        this.bootstrapServers = bootstrapServers;
    }

    public Map<String, Object> overview() throws Exception {
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {

            Set<String> allTopics = admin.listTopics().names().get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<String> dltTopics = allTopics.stream()
                    .filter(t -> t.endsWith(".DLT"))
                    .sorted()
                    .toList();

            Map<TopicPartition, OffsetAndMetadata> replayCommitted = committedOffsets(admin, REPLAY_GROUP);

            List<Map<String, Object>> dlts = new ArrayList<>();
            for (String dlt : dltTopics) {
                dlts.add(describeDlt(admin, dlt, replayCommitted));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dlts", dlts);
            result.put("groups", describeGroups(admin));
            result.put("totalPending", dlts.stream().mapToLong(d -> (long) d.get("pending")).sum());
            return result;
        }
    }

    private Map<String, Object> describeDlt(AdminClient admin, String topic,
                                            Map<TopicPartition, OffsetAndMetadata> replayCommitted) throws Exception {
        List<TopicPartition> tps = partitionsOf(admin, topic);

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliest = listOffsets(admin, tps, OffsetSpec.earliest());
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latest = listOffsets(admin, tps, OffsetSpec.latest());

        long total = 0;
        long pending = 0;
        for (TopicPartition tp : tps) {
            long begin = earliest.get(tp).offset();
            long end = latest.get(tp).offset();
            OffsetAndMetadata committed = replayCommitted.get(tp);
            long handledUpTo = committed != null ? Math.max(committed.offset(), begin) : begin;
            total += end - begin;
            pending += end - handledUpTo;
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("topic", topic);
        dto.put("originalTopic", topic.substring(0, topic.length() - ".DLT".length()));
        dto.put("pending", pending);
        dto.put("total", total);
        dto.put("lastMessageAt", lastMessageTimestamp(admin, tps));
        return dto;
    }

    /** Newest record timestamp on the topic, or null if it is empty. */
    private String lastMessageTimestamp(AdminClient admin, List<TopicPartition> tps) {
        try {
            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> maxTs =
                    listOffsets(admin, tps, OffsetSpec.maxTimestamp());
            long ts = maxTs.values().stream()
                    .mapToLong(ListOffsetsResult.ListOffsetsResultInfo::timestamp)
                    .max()
                    .orElse(-1);
            return ts > 0 ? Instant.ofEpochMilli(ts).toString() : null;
        } catch (Exception e) {
            logger.debug("maxTimestamp lookup failed: {}", e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> describeGroups(AdminClient admin) throws Exception {
        List<String> groupIds = admin.listConsumerGroups().all()
                .get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS).stream()
                .map(g -> g.groupId())
                .filter(id -> !REPLAY_GROUP.equals(id) && !PEEK_GROUP.equals(id))
                .sorted()
                .toList();

        Map<String, ConsumerGroupDescription> descriptions =
                admin.describeConsumerGroups(groupIds).all().get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        List<Map<String, Object>> groups = new ArrayList<>();
        for (String groupId : groupIds) {
            Map<TopicPartition, OffsetAndMetadata> committed = committedOffsets(admin, groupId);
            // lag is only meaningful against main topics; a group never lags on a DLT
            List<TopicPartition> tps = committed.keySet().stream()
                    .filter(tp -> !tp.topic().endsWith(".DLT"))
                    .toList();

            Map<String, Long> lagByTopic = new LinkedHashMap<>();
            if (!tps.isEmpty()) {
                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latest = listOffsets(admin, tps, OffsetSpec.latest());
                for (TopicPartition tp : tps) {
                    long lag = Math.max(0, latest.get(tp).offset() - committed.get(tp).offset());
                    lagByTopic.merge(tp.topic(), lag, Long::sum);
                }
            }

            ConsumerGroupDescription description = descriptions.get(groupId);
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("groupId", groupId);
            dto.put("state", description != null ? description.state().toString() : "UNKNOWN");
            dto.put("totalLag", lagByTopic.values().stream().mapToLong(Long::longValue).sum());
            dto.put("topics", lagByTopic.entrySet().stream()
                    .map(e -> Map.of("topic", e.getKey(), "lag", e.getValue()))
                    .toList());
            groups.add(dto);
        }
        return groups;
    }

    /** Pending (not yet replayed) messages on a DLT, oldest first. */
    public List<Map<String, Object>> peekDlt(String topic, int limit) throws Exception {
        requireDltTopic(topic);
        int cappedLimit = Math.min(Math.max(limit, 1), MAX_PEEK);

        Map<TopicPartition, OffsetAndMetadata> replayCommitted;
        try (AdminClient admin = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            replayCommitted = committedOffsets(admin, REPLAY_GROUP);
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        try (KafkaConsumer<byte[], byte[]> consumer = newConsumer(PEEK_GROUP)) {
            List<TopicPartition> tps = assignAllPartitions(consumer, topic);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(tps);
            for (TopicPartition tp : tps) {
                OffsetAndMetadata committed = replayCommitted.get(tp);
                if (committed != null) {
                    consumer.seek(tp, committed.offset());
                } else {
                    consumer.seekToBeginning(List.of(tp));
                }
            }

            long deadline = System.currentTimeMillis() + REPLAY_DEADLINE_MS;
            while (messages.size() < cappedLimit
                    && !reachedEnd(consumer, tps, endOffsets)
                    && System.currentTimeMillis() < deadline) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    if (messages.size() >= cappedLimit) break;
                    messages.add(toMessageDto(record));
                }
            }
        }
        messages.sort(Comparator.comparing(m -> (String) m.get("timestamp")));
        return messages;
    }

    /**
     * Republish pending DLT messages to their original topic and commit the
     * replay group's offsets so they no longer count as pending. Consumers are
     * idempotent, so an event that is replayed twice is a no-op on the second
     * pass.
     */
    public synchronized Map<String, Object> replayDlt(String topic) throws Exception {
        requireDltTopic(topic);
        String defaultTarget = topic.substring(0, topic.length() - ".DLT".length());

        int replayed = 0;
        try (KafkaConsumer<byte[], byte[]> consumer = newConsumer(REPLAY_GROUP);
             KafkaProducer<byte[], byte[]> producer = newProducer()) {

            List<TopicPartition> tps = assignAllPartitions(consumer, topic);
            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(tps);
            Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(Set.copyOf(tps));
            for (TopicPartition tp : tps) {
                OffsetAndMetadata c = committed.get(tp);
                if (c != null) {
                    consumer.seek(tp, c.offset());
                } else {
                    consumer.seekToBeginning(List.of(tp));
                }
            }

            long deadline = System.currentTimeMillis() + REPLAY_DEADLINE_MS;
            while (!reachedEnd(consumer, tps, endOffsets) && System.currentTimeMillis() < deadline) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<byte[], byte[]> record : records) {
                    String target = headerValue(record, "kafka_dlt-original-topic");
                    if (target == null || target.isBlank()) {
                        target = defaultTarget;
                    }
                    // synchronous send: an unreachable broker must fail the
                    // request, not silently drop messages mid-replay
                    producer.send(new ProducerRecord<>(target, record.key(), record.value()))
                            .get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    replayed++;
                }
            }

            Map<TopicPartition, OffsetAndMetadata> newOffsets = new HashMap<>();
            for (TopicPartition tp : tps) {
                newOffsets.put(tp, new OffsetAndMetadata(consumer.position(tp)));
            }
            consumer.commitSync(newOffsets);
        }

        logger.info("Replayed {} DLT messages from {} back to source", replayed, topic);
        return Map.of("topic", topic, "replayed", replayed);
    }

    // ─── helpers ───────────────────────────────────────────────────────────

    private void requireDltTopic(String topic) {
        if (!topic.endsWith(".DLT")) {
            throw new IllegalArgumentException("Not a DLT topic: " + topic);
        }
    }

    private List<TopicPartition> partitionsOf(AdminClient admin, String topic) throws Exception {
        return admin.describeTopics(List.of(topic)).allTopicNames()
                .get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .get(topic).partitions().stream()
                .map(p -> new TopicPartition(topic, p.partition()))
                .toList();
    }

    private Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> listOffsets(
            AdminClient admin, List<TopicPartition> tps, OffsetSpec spec) throws Exception {
        Map<TopicPartition, OffsetSpec> request = tps.stream()
                .collect(Collectors.toMap(tp -> tp, tp -> spec));
        return admin.listOffsets(request).all().get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private Map<TopicPartition, OffsetAndMetadata> committedOffsets(AdminClient admin, String groupId) throws Exception {
        try {
            return admin.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get(API_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // group does not exist yet (nothing ever replayed) — no committed offsets
            return Map.of();
        }
    }

    private KafkaConsumer<byte[], byte[]> newConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }

    private KafkaProducer<byte[], byte[]> newProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private List<TopicPartition> assignAllPartitions(KafkaConsumer<byte[], byte[]> consumer, String topic) {
        List<TopicPartition> tps = consumer.partitionsFor(topic).stream()
                .map(p -> new TopicPartition(topic, p.partition()))
                .toList();
        consumer.assign(tps);
        return tps;
    }

    private boolean reachedEnd(KafkaConsumer<byte[], byte[]> consumer,
                               List<TopicPartition> tps, Map<TopicPartition, Long> endOffsets) {
        return tps.stream().allMatch(tp -> consumer.position(tp) >= endOffsets.get(tp));
    }

    private Map<String, Object> toMessageDto(ConsumerRecord<byte[], byte[]> record) {
        String payload = record.value() != null ? new String(record.value(), java.nio.charset.StandardCharsets.UTF_8) : "";
        if (payload.length() > MAX_PAYLOAD_CHARS) {
            payload = payload.substring(0, MAX_PAYLOAD_CHARS) + "… (truncated)";
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("partition", record.partition());
        dto.put("offset", record.offset());
        dto.put("timestamp", Instant.ofEpochMilli(record.timestamp()).toString());
        dto.put("key", record.key() != null ? new String(record.key(), java.nio.charset.StandardCharsets.UTF_8) : null);
        dto.put("payload", payload);
        dto.put("exceptionMessage", headerValue(record, "kafka_dlt-exception-message"));
        dto.put("originalTopic", headerValue(record, "kafka_dlt-original-topic"));
        return dto;
    }

    private String headerValue(ConsumerRecord<byte[], byte[]> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header != null && header.value() != null
                ? new String(header.value(), java.nio.charset.StandardCharsets.UTF_8)
                : null;
    }
}
