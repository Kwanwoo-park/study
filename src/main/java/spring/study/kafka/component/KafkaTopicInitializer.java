package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component @RequiredArgsConstructor @Slf4j
public class KafkaTopicInitializer implements SmartInitializingSingleton {
    private final Admin admin;
    private final KafkaOperationsProperties properties;

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isProvisionTopics()) return;
        try {
            var descriptions = admin.describeTopics(KafkaTopics.ALL).topicNameValues();
            List<NewTopic> missing = new ArrayList<>();
            for (String topic : KafkaTopics.ALL) {
                try { descriptions.get(topic).get(5, TimeUnit.SECONDS); }
                catch (java.util.concurrent.ExecutionException e) {
                    if (!(e.getCause() instanceof UnknownTopicOrPartitionException)) throw e;
                    missing.add(definition(topic));
                }
            }
            if (!missing.isEmpty()) admin.createTopics(missing).all().get(5, TimeUnit.SECONDS);
            // Existing partition counts and retention policies are intentionally never altered on startup.
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Kafka topic provisioning unavailable; verify topics on the Kafka operations page. errorType={}", e.getClass().getSimpleName());
        }
    }

    public NewTopic definition(String topic) {
        boolean dlt = topic.endsWith(".DLT");
        int partitions = dlt ? properties.getDeadLetterPartitions()
                : KafkaTopics.CHAT.equals(topic) ? properties.getChatPartitions() : properties.getNotificationPartitions();
        return new NewTopic(topic, partitions, properties.getReplicationFactor()).configs(Map.of(
                "cleanup.policy", "delete", "retention.ms", Long.toString(TimeUnit.DAYS.toMillis(dlt ? properties.getDeadLetterRetentionDays() : properties.getRetentionDays())),
                "min.insync.replicas", Integer.toString(properties.getMinInSyncReplicas())));
    }
}
