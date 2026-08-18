package spring.study.aws.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.aws.service.ImageCleanupService;

@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {
    private final ImageCleanupService imageCleanupService;

    @Scheduled(fixedDelayString = "${image.cleanup.fixed-delay-ms:60000}")
    public void cleanupImages() {
        imageCleanupService.processNextBatch();
    }
}
