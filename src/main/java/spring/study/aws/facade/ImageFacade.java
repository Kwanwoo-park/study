package spring.study.aws.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.aws.entity.ImageCleanupTask;
import spring.study.aws.service.ImageCleanupService;
import spring.study.aws.service.ImageS3Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ImageFacade {
    private final ImageS3Service imageS3Service;
    private final ImageCleanupService cleanupService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNextBatch() {
        for (ImageCleanupTask task : cleanupService.findTask(PageRequest.of(0, 100))) {
            try {
                imageS3Service.deleteImage(task.getImageUrl());
                cleanupService.deleteTask(task);
            } catch (Exception exception) {
                task.recordFailure(exception.getMessage());
                log.warn("S3 image cleanup will be retried. taskId={}", task.getId(), exception);
            }
        }
    }
}
