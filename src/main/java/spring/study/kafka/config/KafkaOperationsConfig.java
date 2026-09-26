package spring.study.kafka.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class KafkaOperationsConfig {
    @Bean(destroyMethod = "close")
    public Admin kafkaOperationsAdmin(@Value("${bootstrap-servers}") String servers) {
        return Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000,
                AdminClientConfig.CLIENT_ID_CONFIG, "study-kafka-operations"));
    }
}
