package spring.study.admin.component;

import org.junit.jupiter.api.Test;
import spring.study.admin.dto.SystemResourceSample;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemResourceMonitorTest {
    @Test
    void retainsOnlyRecentSamplesAndUsesTheLatestMeasurementForStatus() {
        SystemResourceMonitor monitor = new SystemResourceMonitor();
        Map<String, Object> first = monitor.status();
        assertThat(monitor.status().get("resourceHistory")).isEqualTo(first.get("resourceHistory"));

        for (int index = 0; index < 65; index++) monitor.sample();

        Map<String, Object> status = monitor.status();
        List<?> history = (List<?>) status.get("resourceHistory");
        assertThat(history).hasSize(61);
        SystemResourceSample latest = (SystemResourceSample) history.get(history.size() - 1);
        assertThat(status.get("sampledAt")).isEqualTo(latest.sampledAt());
        assertThat(((Map<?, ?>) status.get("cpu")).get("systemPercent")).isEqualTo(latest.cpuPercent());
        assertThat(((Map<?, ?>) status.get("memory")).get("usedPercent")).isEqualTo(latest.memoryPercent());
    }
}
