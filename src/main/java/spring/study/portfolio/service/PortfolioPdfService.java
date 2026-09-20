package spring.study.portfolio.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioPdfService {
    private final PortfolioPdfRenderer renderer;
    private final ReentrantLock generationLock = new ReentrantLock();
    private Path cacheDirectory;
    private String cachedVersion;
    private Instant retryAfter = Instant.MIN;
    private volatile boolean closed;

    public byte[] download() {
        boolean locked = false;
        Path workDirectory = null;
        try {
            locked = generationLock.tryLock(50, TimeUnit.SECONDS);
            if (!locked || closed) throw unavailable();
            Map<String, byte[]> sources = loadSources();
            String version = fingerprint(sources, renderer.version());
            Path cachedFile = cacheDirectory == null ? null : cacheDirectory.resolve("portfolio.pdf");
            if (version.equals(cachedVersion) && cachedFile != null && Files.isRegularFile(cachedFile)) return Files.readAllBytes(cachedFile);
            // Failed requests must not repeatedly launch Chromium or serve an outdated PDF.
            if (Instant.now().isBefore(retryAfter)) throw unavailable();
            if (cacheDirectory == null) cacheDirectory = Files.createTempDirectory("study-portfolio-pdf-");
            workDirectory = Files.createTempDirectory(cacheDirectory, "render-");
            Path output = workDirectory.resolve("portfolio.pdf");
            renderer.render(sources, output);
            if (!Files.isRegularFile(output) || Files.size(output) > 20 * 1024 * 1024) throw new IOException("Invalid portfolio PDF output");
            byte[] bytes = Files.readAllBytes(output);
            if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) throw new IOException("Renderer did not produce a PDF");
            // Same filesystem, atomic publish: failed/partial output never replaces the cache.
            Files.move(output, cacheDirectory.resolve("portfolio.pdf"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            cachedVersion = version;
            retryAfter = Instant.MIN;
            log.info("Portfolio PDF generated and cached ({} bytes)", bytes.length);
            return bytes;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException error) {
            retryAfter = Instant.now().plusSeconds(30);
            log.warn("Portfolio PDF generation unavailable", error);
            throw unavailable();
        } finally {
            cleanup(workDirectory);
            if (locked) generationLock.unlock();
        }
    }

    Map<String, byte[]> loadSources() throws IOException {
        Map<String, byte[]> sources = new TreeMap<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // Resolve the deployed classpath, including resources inside an executable Spring Boot JAR.
        for (String pattern : new String[]{"classpath*:/static/portfolio/**/*.*", "classpath*:/static/css/common/page-back.css", "classpath*:/static/js/common/page-back.js"}) {
            for (Resource resource : resolver.getResources(pattern)) {
                String url = resource.getURL().toExternalForm();
                String name = url.substring(url.lastIndexOf("/static/") + "/static/".length());
                try (InputStream input = resource.getInputStream()) {
                    sources.put(name, input.readAllBytes());
                }
            }
        }
        if (!sources.containsKey("portfolio/index.html")) throw new IOException("Deployed portfolio resources are missing");
        return sources;
    }

    private String fingerprint(Map<String, byte[]> sources, byte[] rendererVersion) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            sources.forEach((name, bytes) -> {
                digest.update(name.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(bytes);
                digest.update((byte) 0);
            });
            digest.update(rendererVersion);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "PDF를 준비하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private void cleanup(Path directory) {
        if (directory == null) return;
        try {
            FileSystemUtils.deleteRecursively(directory);
        } catch (IOException error) {
            log.warn("Could not remove portfolio PDF temporary directory {}", directory, error);
        }
    }

    @PreDestroy
    public void close() {
        closed = true;
        renderer.close();
        generationLock.lock();
        try {
            cleanup(cacheDirectory);
            cacheDirectory = null;
            cachedVersion = null;
        } finally {
            generationLock.unlock();
        }
    }
}
