package spring.study.aws.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.aws.facade.ImageFacade;

@Component
@RequiredArgsConstructor
public class ImageCleanupScheduler {
    private final ImageFacade imageFacade;

    @Scheduled(fixedDelayString = "${image.cleanup.fixed-delay-ms:60000}")
    public void cleanupImages() {
        imageFacade.processNextBatch();
    }
}
