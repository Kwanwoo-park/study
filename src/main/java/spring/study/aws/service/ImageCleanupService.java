package spring.study.aws.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
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

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.count();
    }

    @Transactional
    public void enqueue(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank())
            repository.save(ImageCleanupTask
                    .builder()
                    .imageUrl(imageUrl)
                    .build());
    }

    @Transactional
    public void enqueueAll(Collection<String> imageUrls) {
        if (imageUrls == null)
            return;

        List<ImageCleanupTask> tasks = imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .map(ImageCleanupTask::new)
                .toList();

        repository.saveAll(tasks);
    }

    @Transactional(readOnly = true)
    public List<ImageCleanupTask> findTask(PageRequest request) {
        return repository.findNextBatchForUpdate(request);
    }

    @Transactional
    public void deleteTask(ImageCleanupTask task) {
        repository.delete(task);
    }
}
