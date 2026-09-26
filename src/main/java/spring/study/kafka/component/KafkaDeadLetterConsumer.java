package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import spring.study.kafka.config.KafkaTopics;
import spring.study.kafka.service.KafkaDeadLetterService;

@Component @RequiredArgsConstructor
public class KafkaDeadLetterConsumer {
    private final KafkaDeadLetterService service;
    @KafkaListener(topics = {KafkaTopics.CHAT_DLT, KafkaTopics.NOTIFICATION_DLT}, containerFactory = "deadLetterKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, byte[]> record) { service.archive(record); }
}
