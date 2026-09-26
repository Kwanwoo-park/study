package spring.study.kafka.component;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.Test;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.kafka.config.KafkaTopics;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class KafkaTopicInitializerTest {
    @Test void neverChangesExistingTopicPartitionsOrPolicies() {
        Admin admin = mock(Admin.class);
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        Map<String, KafkaFuture<TopicDescription>> topics = new HashMap<>();
        KafkaTopics.ALL.forEach(topic -> topics.put(topic, KafkaFuture.completedFuture(null)));
        when(admin.describeTopics(KafkaTopics.ALL)).thenReturn(result);
        when(result.topicNameValues()).thenReturn(topics);
        var properties = new KafkaOperationsProperties(); properties.setChatPartitions(3);
        var initializer = new KafkaTopicInitializer(admin, properties);
        initializer.afterSingletonsInstantiated();
        verify(admin).describeTopics(KafkaTopics.ALL);
        verifyNoMoreInteractions(admin);
        assertThat(initializer.definition(KafkaTopics.CHAT).numPartitions()).isEqualTo(3);
        assertThat(initializer.definition(KafkaTopics.CHAT_DLT).configs().get("cleanup.policy")).isEqualTo("delete");
    }
}
