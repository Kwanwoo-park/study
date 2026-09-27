package spring.study.admin.service;

import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.common.service.OnlineUserService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.kafka.service.KafkaQueueQueryService;
import spring.study.aws.service.ImageCleanupService;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminSystemStatusService {
    private final OnlineUserService onlineUserService;
    private final ChatPresenceService chatPresenceService;
    private final KafkaQueueQueryService kafkaQueueQueryService;
    private final ImageCleanupService imageCleanupService;

    public Map<String, Object> snapshot() {
        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        File root = new File("/");

        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = Math.max(0L, totalMemory - freeMemory);
        long maxHeap = runtime.maxMemory();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        long totalDisk = root.getTotalSpace();
        long freeDisk = root.getFreeSpace();
        long usedDisk = Math.max(0L, totalDisk - freeDisk);

        Map<String, Object> status = new HashMap<>();
        status.put("result", 1L);
        status.put("cpu", Map.of(
                "systemPercent", percent(osBean.getCpuLoad()),
                "processPercent", percent(osBean.getProcessCpuLoad()),
                "cores", osBean.getAvailableProcessors()
        ));
        status.put("memory", Map.of(
                "usedBytes", usedMemory,
                "totalBytes", totalMemory,
                "usedPercent", percent(usedMemory, totalMemory)
        ));
        status.put("disk", Map.of(
                "usedBytes", usedDisk,
                "totalBytes", totalDisk,
                "usedPercent", percent(usedDisk, totalDisk)
        ));
        status.put("jvm", Map.of(
                "heapUsedBytes", usedHeap,
                "heapMaxBytes", maxHeap,
                "heapUsedPercent", percent(usedHeap, maxHeap),
                "uptimeMillis", runtimeMxBean.getUptime(),
                "uptime", formatDuration(Duration.ofMillis(runtimeMxBean.getUptime())),
                "startedAt", LocalDateTime.now().minus(Duration.ofMillis(runtimeMxBean.getUptime())).toString()
        ));
        status.put("traffic", Map.of(
                "activeSessions", onlineUserService.countOnlineMembers(),
                "activeWebSockets", chatPresenceService.countActiveConnections()
        ));
        var kafka = kafkaQueueQueryService.counts();
        status.put("queues", Map.of(
                "kafkaPending", kafka.pending(),
                "kafkaDeadLetters", kafka.failed(),
                "imageCleanupPending", imageCleanupService.countPending()
        ));

        return status;
    }

    private double percent(double ratio) {
        if (Double.isNaN(ratio) || ratio < 0) {
            return 0D;
        }

        return Math.round(ratio * 1000D) / 10D;
    }

    private double percent(long used, long total) {
        if (total <= 0) {
            return 0D;
        }

        return Math.round((used * 1000D) / total) / 10D;
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return days + "일 " + hours + "시간 " + minutes + "분";
        }

        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }

        return minutes + "분";
    }
}
