package spring.study.portfolio.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class PortfolioPdfRenderer {
    private String rendererDirectory = "study/tools/portfolio-pdf";
    private String nodeExecutable = "node";
    private volatile Process activeProcess;
    private boolean closed;

    public byte[] version() throws IOException {
        Path directory = Path.of(rendererDirectory).toAbsolutePath();
        return (Files.readString(directory.resolve("generate.mjs")) + Files.readString(directory.resolve("package-lock.json"))).getBytes(StandardCharsets.UTF_8);
    }

    public void render(Map<String, byte[]> sources, Path output) throws IOException, InterruptedException {
        Path staticRoot = output.getParent().resolve("static");
        for (Map.Entry<String, byte[]> source : sources.entrySet()) {
            Path target = staticRoot.resolve(source.getKey()).normalize();
            if (!target.startsWith(staticRoot)) throw new IOException("Invalid portfolio resource path");
            Files.createDirectories(target.getParent());
            Files.write(target, source.getValue());
        }
        Path script = Path.of(rendererDirectory).toAbsolutePath().resolve("generate.mjs");
        Path log = output.getParent().resolve("renderer.log");
        Process process = start(script, output, staticRoot, log);
        try {
            if (!process.waitFor(45, TimeUnit.SECONDS)) throw new IOException("Portfolio PDF rendering timed out");
            if (process.exitValue() != 0) {
                String message = Files.readString(log);
                throw new IOException("Portfolio PDF renderer failed: " + message.substring(0, Math.min(message.length(), 8000)));
            }
        } finally {
            stop(process);
            activeProcess = null;
        }
    }

    private synchronized Process start(Path script, Path output, Path staticRoot, Path log) throws IOException {
        if (closed) throw new IOException("Portfolio PDF renderer is shutting down");
        activeProcess = new ProcessBuilder(nodeExecutable, script.toString(), output.toString(), staticRoot.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        return activeProcess;
    }

    private void stop(Process process) {
        if (process != null && process.isAlive()) {
            var children = process.descendants().toList();
            children.forEach(child -> child.destroyForcibly());
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @PreDestroy
    public synchronized void close() {
        closed = true;
        stop(activeProcess);
    }
}
