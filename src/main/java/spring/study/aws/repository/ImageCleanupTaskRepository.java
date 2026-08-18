package spring.study.aws.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import spring.study.aws.entity.ImageCleanupTask;

import java.util.List;

public interface ImageCleanupTaskRepository extends JpaRepository<ImageCleanupTask, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from ImageCleanupTask task order by task.id")
    List<ImageCleanupTask> findNextBatchForUpdate(Pageable pageable);
}
