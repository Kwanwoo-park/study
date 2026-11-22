package spring.study.kafka.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.Map;

@Configuration
public class KafkaOperationsConfig {
    @Bean
    public String kafkaBootstrapServers(Environment environment) {
        String servers = environment.getProperty("spring.kafka.bootstrap-servers");
        if (servers == null) servers = environment.getProperty("bootstrap-servers");

        Assert.hasText(servers, "Configure spring.kafka.bootstrap-servers or bootstrap-servers");
        return servers;
    }

    @Bean(destroyMethod = "close")
    public Admin kafkaOperationsAdmin(@Qualifier("kafkaBootstrapServers") String servers) {
        return Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000,
                AdminClientConfig.CLIENT_ID_CONFIG, "study-kafka-operations"));
    }
}
