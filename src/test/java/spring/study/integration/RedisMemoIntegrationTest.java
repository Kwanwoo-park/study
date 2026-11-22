package spring.study.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import spring.study.memo.dto.MemoDraft;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.entity.MemoColor;
import spring.study.memo.repository.MemoDraftRepository;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("infrastructure")
class RedisMemoIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void unflushedDraftAndDueEntrySurviveRedisRestart() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        var time = LocalDateTime.now();
        long now = System.currentTimeMillis();
        var response = new MemoResponseDto(1L, "제목", "DB에 반영하기 전의 내용", MemoColor.MINT, false, 2L, time, time);
        var draft = new MemoDraft(response, 9L, 1L, now, now, 0, 0, false, false, "operation-1");

        try (RedisServer server = start(port)) {
            var repository = server.repository();
            repository.requireDurability();
            assertThat(repository.lock(1L, "test-lock", 30000)).isTrue();
            repository.store(draft, "test-lock", now, 600);
            repository.unlock(1L, "test-lock");
        }

        try (RedisServer server = start(port)) {
            var repository = server.repository();
            repository.requireDurability();
            assertThat(repository.find(1L)).contains(draft);
            assertThat(repository.due(now, 5)).containsExactly(1L);
        }
    }

    private RedisServer start(int port) throws Exception {
        String executable = System.getenv().getOrDefault("STUDY_TEST_REDIS_EXECUTABLE", "redis-server");
        Process process = new ProcessBuilder(executable, "--bind", "127.0.0.1", "--port", Integer.toString(port),
                "--dir", directory.toString(), "--appendonly", "yes", "--appendfsync", "always",
                "--maxmemory-policy", "noeviction")
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(directory.resolve("redis.log").toFile()))
                .start();

        try {
            awaitPort(process, port);
            var factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", port));
            factory.afterPropertiesSet();
            return new RedisServer(process, factory);
        } catch (Exception exception) {
            stop(process);
            throw exception;
        }
    }

    private static void awaitPort(Process process, int port) throws Exception {
        for (int attempt = 0; attempt < 200 && process.isAlive(); attempt++) {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return;
            } catch (IOException exception) {
                Thread.sleep(25);
            }
        }
        throw new IllegalStateException("Test Redis did not open its loopback port");
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
        }
    }

    private record RedisServer(Process process, LettuceConnectionFactory connection) implements AutoCloseable {
        MemoDraftRepository repository() {
            return new MemoDraftRepository(new StringRedisTemplate(connection), new ObjectMapper().findAndRegisterModules());
        }

        @Override
        public void close() throws InterruptedException {
            try {
                connection.destroy();
            } finally {
                stop(process);
            }
        }
    }
}
