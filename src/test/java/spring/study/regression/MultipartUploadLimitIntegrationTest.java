package spring.study.regression;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import spring.study.admin.repository.AdminFileRepository;
import spring.study.admin.service.AdminFileS3Storage;
import spring.study.admin.service.AdminFileService;
import spring.study.admin.service.SystemIncidentService;
import spring.study.aws.service.ImageS3Service;
import spring.study.common.component.GlobalExceptionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Real HTTP/Tomcat parsing is required: MockMvc multipart() bypasses servlet size limits.
@SpringBootTest(classes = MultipartUploadLimitIntegrationTest.Config.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.config.location=optional:classpath:/multipart-limit-test-only.properties")
class MultipartUploadLimitIntegrationTest {
    private static final int MIB = 1024 * 1024;
    @LocalServerPort int port;
    @Autowired AdminFileS3Storage adminStorage;
    @Autowired AmazonS3 imageStorage;

    @DynamicPropertySource
    static void uploadLimits(DynamicPropertyRegistry registry) throws IOException {
        Map<String, Object> settings = documentedUploadSettings();
        registry.add("spring.servlet.multipart.max-file-size", () -> settings.get("spring.servlet.multipart.max-file-size"));
        registry.add("spring.servlet.multipart.max-request-size", () -> settings.get("spring.servlet.multipart.max-request-size"));
    }

    @BeforeEach
    void clearStorageCalls() {
        clearInvocations(adminStorage, imageStorage);
    }

    @Test
    void documentedSettingsUseTwentyMiBPerFileAndTwoHundredTenMiBPerRequest() throws IOException {
        Map<String, Object> settings = documentedUploadSettings();
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("upload-settings", settings));
        MultipartProperties properties = Binder.get(environment).bind("spring.servlet.multipart", MultipartProperties.class).get();
        assertThat(properties.getMaxFileSize().toBytes()).isEqualTo(20L * MIB);
        assertThat(properties.getMaxRequestSize().toBytes()).isEqualTo(210L * MIB);
    }

    @Test
    void administratorCanUploadMoreThanTenMiB() throws Exception {
        assertThat(upload("/test/admin-files", 11 * MIB).statusCode()).isEqualTo(201);
        verify(adminStorage).upload(anyString(), anyString(), eq(11L * MIB), any());
        verifyNoInteractions(imageStorage);
    }

    @Test
    void administratorCanUploadExactlyTwentyMiB() throws Exception {
        assertThat(upload("/test/admin-files", 20 * MIB).statusCode()).isEqualTo(201);
        verify(adminStorage).upload(anyString(), anyString(), eq(20L * MIB), any());
    }

    @Test
    void servletRejectsAnAdminFileOneByteOverTwentyMiB() throws Exception {
        assertThat(upload("/test/admin-files", 20 * MIB + 1).statusCode()).isEqualTo(413);
        verifyNoInteractions(adminStorage);
    }

    @Test
    void imageExactlyTwentyMiBIsAccepted() throws Exception {
        assertThat(upload("/test/images", 20 * MIB).statusCode()).isEqualTo(200);
        verify(imageStorage).putObject(any(com.amazonaws.services.s3.model.PutObjectRequest.class));
    }

    @Test
    void servletRejectsAnImageOneByteOverTwentyMiB() throws Exception {
        HttpResponse<String> response = upload("/test/images", 20 * MIB + 1);
        assertThat(response.statusCode()).isEqualTo(413);
        verifyNoInteractions(imageStorage);
    }

    @Test
    void requestTotalIsStillLimitedEvenWhenEachPartIsSmallEnough() throws Exception {
        // 211MiB in individually valid parts exceeds the real 210MiB request limit.
        int[] sizes = new int[11];
        Arrays.fill(sizes, 20 * MIB);
        sizes[10] = 11 * MIB;
        assertThat(upload("/test/admin-files", sizes).statusCode()).isEqualTo(413);
        verifyNoInteractions(adminStorage);
    }

    private HttpResponse<String> upload(String path, int... sizes) throws Exception {
        String boundary = "upload-limit-regression-boundary";
        List<HttpRequest.BodyPublisher> parts = new ArrayList<>();
        for (int size : sizes) {
            parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"upload.png\"\r\nContent-Type: application/octet-stream\r\n\r\n", StandardCharsets.UTF_8));
            // Generate chunked data lazily instead of allocating a 211MiB request in memory.
            parts.add(HttpRequest.BodyPublishers.ofInputStream(() -> new ZeroInputStream(size)));
            parts.add(HttpRequest.BodyPublishers.ofString("\r\n"));
        }
        parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "--\r\n"));
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(publisher).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> documentedUploadSettings() throws IOException {
        Map<String, Object> settings = new HashMap<>();
        // Deployment properties contain secrets and are gitignored; verify the tracked example.
        for (String line : Files.readAllLines(Path.of("docs/admin-file-storage.md"))) {
            if (line.startsWith("spring.servlet.multipart.max-")) {
                int separator = line.indexOf('=');
                settings.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        return settings;
    }

    private static class ZeroInputStream extends InputStream {
        private int remaining;

        ZeroInputStream(int remaining) {
            this.remaining = remaining;
        }

        @Override
        public int read() {
            if (remaining == 0) return -1;
            remaining--;
            return 0;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) {
            if (length == 0) return 0;
            if (remaining == 0) return -1;
            int count = Math.min(remaining, length);
            Arrays.fill(bytes, offset, offset + count, (byte) 0);
            remaining -= count;
            return count;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class, MultipartAutoConfiguration.class, JacksonAutoConfiguration.class})
    @Import({UploadController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean
        AmazonS3 imageStorage() throws Exception {
            AmazonS3 storage = mock(AmazonS3.class);
            when(storage.getUrl(anyString(), anyString())).thenReturn(new URL("https://example.test/image.png"));
            return storage;
        }

        @Bean
        ImageS3Service imageService(AmazonS3 storage) {
            ImageS3Service service = new ImageS3Service(storage);
            ReflectionTestUtils.setField(service, "bucketName", "test-images");
            return service;
        }

        @Bean
        AdminFileS3Storage adminStorage() {
            return mock(AdminFileS3Storage.class);
        }

        @Bean
        AdminFileService adminService(AdminFileS3Storage storage) {
            AdminFileRepository repository = mock(AdminFileRepository.class);
            when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
            PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
            when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
            return new AdminFileService(repository, transactions, storage);
        }

        @Bean
        SystemIncidentService systemIncidentService() {
            return mock(SystemIncidentService.class);
        }
    }

    @RestController
    static class UploadController {
        private final ImageS3Service images;
        private final AdminFileService admin;

        UploadController(ImageS3Service images, AdminFileService admin) {
            this.images = images;
            this.admin = admin;
        }

        @PostMapping("/test/admin-files")
        ResponseEntity<?> adminUpload(@RequestParam("file") MultipartFile file) {
            return ResponseEntity.status(201).body(admin.upload(file, 7L));
        }

        @PostMapping("/test/images")
        ResponseEntity<?> imageUpload(@RequestParam("file") MultipartFile file) throws IOException {
            return ResponseEntity.ok(images.uploadImageToS3(file));
        }
    }
}
