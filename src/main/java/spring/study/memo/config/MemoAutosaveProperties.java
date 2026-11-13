package spring.study.memo.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "memo.autosave")
public class MemoAutosaveProperties {
    @Min(100) private long pollMs = 1000;
    @Min(1000) private long idleMs = 10000;
    @Min(1000) private long maxDirtyMs = 60000;
    @Min(60) private long cleanTtlSeconds = 600;
    @Min(30000) private long lockMs = 30000;
    @Min(1) private int batchSize = 50;
    @Min(1000) private long retryMaxMs = 60000;
    private boolean requireDurableRedis = true;
}
