package spring.study.aws.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import spring.study.aws.entity.ImageCleanupTask;
import spring.study.aws.repository.ImageCleanupTaskRepository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupService {
    private final ImageCleanupTaskRepository repository;
    private final ImageS3Service imageS3Service;

    @Transactional
    public void enqueue(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) repository.save(new ImageCleanupTask(imageUrl));
    }

    @Transactional
    public void enqueueAll(Collection<String> imageUrls) {
        if (imageUrls == null) return;
        List<ImageCleanupTask> tasks = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .map(ImageCleanupTask::new)
                .toList();
        repository.saveAll(tasks);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNextBatch() {
        for (ImageCleanupTask task : repository.findNextBatchForUpdate(PageRequest.of(0, 100))) {
            try {
                imageS3Service.deleteImage(task.getImageUrl());
                repository.delete(task);
            } catch (Exception exception) {
                task.recordFailure(exception.getMessage());
                log.warn("S3 image cleanup will be retried. taskId={}", task.getId(), exception);
            }
        }
    }
}
