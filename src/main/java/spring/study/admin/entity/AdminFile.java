package spring.study.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "admin_file", indexes = @Index(name = "idx_admin_file_created", columnList = "created_at,id"))
public class AdminFile {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private Long uploadedBy;

    @Column(name = "s3_version_id", length = 1024)
    private String s3VersionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public AdminFile(String id, String originalFilename, long size, Long uploadedBy) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.size = size;
        this.uploadedBy = uploadedBy;
        this.createdAt = LocalDateTime.now();
    }

    public void pinVersion(String versionId) {
        this.s3VersionId = versionId;
    }

    public void recordUpload(long size, Long uploadedBy, String versionId) {
        this.size = size;
        this.uploadedBy = uploadedBy;
        this.s3VersionId = versionId;
    }

}
