package spring.study.admin.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class SystemDiagnosticsService {
    private static final int PROCESS_LIMIT = 100;
    private static final int FILE_PAGE_SIZE = 100;

    public record ProcessRow(long pid, String name, long memoryBytes) {}
    public record ProcessSnapshot(int totalCount, List<ProcessRow> processes) {}
    public record DiskRoot(String id, String label) {}
    public record FileRow(String name, boolean directory, long sizeBytes, Instant modifiedAt) {}
    public record DiskSnapshot(List<DiskRoot> roots, String root, String path, int page,
                               int pageSize, int totalCount, List<FileRow> entries) {}

    public ProcessSnapshot processes() {
        Process process = null;
        try {
            // `comm` contains the executable name, never command-line arguments or credentials.
            process = new ProcessBuilder("ps", "-eo", "pid=,rss=,comm=")
                    .redirectErrorStream(true).start();
            Process queriedProcess = process;
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                if (queriedProcess.isAlive()) queriedProcess.destroyForcibly();
            });
            List<ProcessRow> rows = new ArrayList<>();
            try (BufferedReader reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ProcessRow row = parseProcessLine(line);
                    if (row != null) rows.add(row);
                }
            }
            if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
                throw new IllegalStateException("프로세스 목록을 조회할 수 없습니다.");
            }
            rows.sort(Comparator.comparingLong(ProcessRow::memoryBytes).reversed()
                    .thenComparingLong(ProcessRow::pid));
            return new ProcessSnapshot(rows.size(), rows.stream().limit(PROCESS_LIMIT).toList());
        } catch (IOException e) {
            throw new IllegalStateException("프로세스 목록을 조회할 수 없습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("프로세스 조회가 중단되었습니다.", e);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }

    static ProcessRow parseProcessLine(String line) {
        String[] parts = line.trim().split("\\s+", 3);
        if (parts.length != 3) return null;
        try {
            long pid = Long.parseLong(parts[0]);
            long memoryBytes = Math.multiplyExact(Long.parseLong(parts[1]), 1024L);
            if (pid <= 0 || memoryBytes < 0) return null;
            String name = Paths.get(parts[2]).getFileName().toString();
            return new ProcessRow(pid, name, memoryBytes);
        } catch (ArithmeticException | IllegalArgumentException e) {
            return null;
        }
    }

    public DiskSnapshot disk(String rootId, String relativePath, int page) {
        if (page < 0 || page > 10000) throw new IllegalArgumentException("잘못된 페이지입니다.");
        List<RootPath> available = availableRoots();
        RootPath selected = available.stream().filter(root -> root.id().equals(rootId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("조회할 수 없는 경로입니다."));
        String path = relativePath == null ? "" : relativePath;
        Path relative = Paths.get(path);
        if (relative.isAbsolute() || relative.toString().contains("\\")) {
            throw new IllegalArgumentException("조회할 수 없는 경로입니다.");
        }
        Path current = selected.path().resolve(relative).normalize();
        if (!current.startsWith(selected.path())) {
            throw new IllegalArgumentException("조회할 수 없는 경로입니다.");
        }
        Path cursor = selected.path();
        for (Path part : selected.path().relativize(current)) {
            cursor = cursor.resolve(part);
            if (Files.isSymbolicLink(cursor)) {
                throw new IllegalArgumentException("심볼릭 링크는 탐색할 수 없습니다.");
            }
        }
        if (!Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("조회할 수 없는 디렉터리입니다.");
        }
        try (Stream<Path> stream = Files.list(current)) {
            List<Path> children = stream.filter(p -> !Files.isSymbolicLink(p))
                    .sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
                            .thenComparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            long start = (long) page * FILE_PAGE_SIZE;
            List<FileRow> entries = children.stream().skip(start).limit(FILE_PAGE_SIZE)
                    .map(this::toFileRow).toList();
            return new DiskSnapshot(available.stream().map(r -> new DiskRoot(r.id(), r.label())).toList(),
                    selected.id(), selected.path().relativize(current).toString(), page,
                    FILE_PAGE_SIZE, children.size(), entries);
        } catch (IOException | SecurityException e) {
            throw new IllegalStateException("디스크 목록을 조회할 수 없습니다.", e);
        }
    }

    private FileRow toFileRow(Path path) {
        boolean directory = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        long size = 0;
        Instant modified = null;
        try {
            if (!directory) size = Files.size(path);
            modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant();
        } catch (IOException | SecurityException ignored) {
            // The file may disappear between listing and reading its metadata.
        }
        return new FileRow(path.getFileName().toString(), directory, size, modified);
    }

    private record RootPath(String id, String label, Path path) {}

    private List<RootPath> availableRoots() {
        List<RootPath> roots = new ArrayList<>();
        addRoot(roots, "project", "실행 프로젝트", Path.of(System.getProperty("user.dir")));
        addRoot(roots, "commands", "설치된 실행 파일 (/usr/bin)", Path.of("/usr/bin"));
        addRoot(roots, "optional", "추가 설치 프로그램 (/opt)", Path.of("/opt"));
        addRoot(roots, "local", "로컬 설치 프로그램 (/usr/local/bin)", Path.of("/usr/local/bin"));
        addRoot(roots, "macapps", "macOS 응용 프로그램", Path.of("/Applications"));
        return roots;
    }

    private void addRoot(List<RootPath> roots, String id, String label, Path path) {
        try {
            if (Files.isDirectory(path)) roots.add(new RootPath(id, label, path.toRealPath()));
        } catch (IOException | SecurityException ignored) {
            // Unavailable roots are not offered to the client.
        }
    }
}
