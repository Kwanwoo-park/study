package spring.study.aws.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "image_cleanup_task")
@NoArgsConstructor
public class ImageCleanupTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    public ImageCleanupTask(String imageUrl) {
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }

    public void recordFailure(String error) {
        attemptCount++;
        lastError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 1000));
    }
}
