package spring.study.memo.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class MemoRedisConfig {
    @Bean
    public LettuceConnectionFactory memoRedisConnectionFactory(
            @Value("${memo.redis.host:${spring.data.redis.host}}") String host,
            @Value("${memo.redis.port:${spring.data.redis.port}}") int port,
            @Value("${memo.redis.database:0}") int database,
            @Value("${memo.redis.username:${spring.data.redis.username:}}") String username,
            @Value("${memo.redis.password:${spring.data.redis.password:}}") String password) {
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(host, port);
        server.setDatabase(database);
        if (!username.isBlank()) server.setUsername(username);
        if (!password.isBlank()) server.setPassword(RedisPassword.of(password));
        LettuceClientConfiguration client = LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(3)).build();
        return new LettuceConnectionFactory(server, client);
    }

    @Bean
    public RedisTemplate<String, String> memoRedisTemplate(@Qualifier("memoRedisConnectionFactory") LettuceConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
