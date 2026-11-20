package spring.study.admin.component;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.admin.dto.SystemResourceSample;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SystemResourceMonitor {
    private static final long SAMPLE_INTERVAL_MILLIS = 30_000;
    private static final long HISTORY_DURATION_MILLIS = 30 * 60_000L;
    private static final int MAX_SAMPLES = (int) (HISTORY_DURATION_MILLIS / SAMPLE_INTERVAL_MILLIS) + 1;

    private final OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    private final Deque<SystemResourceSample> history = new ArrayDeque<>();
    private Map<String, Object> latest;

    @Scheduled(fixedDelay = SAMPLE_INTERVAL_MILLIS)
    public synchronized void sample() {
        long sampledAt = System.currentTimeMillis();
        Double cpuPercent = percent(osBean.getCpuLoad());
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        long usedMemory = 0L;
        Double memoryPercent = null;
        if (totalMemory > 0 && freeMemory >= 0 && freeMemory <= totalMemory) {
            usedMemory = totalMemory - freeMemory;
            memoryPercent = percent((double) usedMemory / totalMemory);
        }

        Map<String, Object> cpu = new HashMap<>();
        cpu.put("systemPercent", cpuPercent);
        cpu.put("processPercent", percent(osBean.getProcessCpuLoad()));
        cpu.put("cores", osBean.getAvailableProcessors());

        Map<String, Object> memory = new HashMap<>();
        memory.put("usedBytes", usedMemory);
        memory.put("totalBytes", totalMemory);
        memory.put("usedPercent", memoryPercent);
        latest = Map.of("cpu", cpu, "memory", memory, "sampledAt", sampledAt);

        history.removeIf(point -> point.sampledAt() < sampledAt - HISTORY_DURATION_MILLIS || point.sampledAt() > sampledAt);
        if (history.size() == MAX_SAMPLES) history.removeFirst();
        history.addLast(new SystemResourceSample(sampledAt, cpuPercent, memoryPercent));
    }

    public synchronized Map<String, Object> status() {
        if (latest == null) sample();

        Map<String, Object> status = new HashMap<>(latest);
        status.put("resourceHistory", List.copyOf(history));
        return status;
    }

    private Double percent(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0 || ratio > 1) return null;
        return Math.round(ratio * 1000D) / 10D;
    }
}
