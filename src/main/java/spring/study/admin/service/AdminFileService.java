package spring.study.admin.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.AdminFileResponseDto;
import spring.study.admin.dto.Download;
import spring.study.admin.entity.AdminFile;
import spring.study.admin.repository.AdminFileRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileService {
    private static final String PREFIX = "admin-files/";

    private final AdminFileRepository adminFileRepository;
    private final AmazonS3 amazonS3;

    @Value("${admin.files.s3.bucket}")
    private String bucket;

    @Transactional(readOnly = true)
    public Page<AdminFileResponseDto> list(int page) {
        if (page < 0 || page > 1000000) throw new IllegalArgumentException("올바른 페이지 번호를 입력해 주세요");
        return adminFileRepository.findAll(PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))).map(AdminFileResponseDto::from);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pinExistingVersion(String filename) {
        adminFileRepository.findFirstByOriginalFilenameOrderByCreatedAtDescIdDesc(safeFilename(filename))
                .filter(stored -> stored.getS3VersionId() == null)
                .ifPresent(stored -> {
                    requirePrivateBucket();
                    try {
                        String versionId = amazonS3.getObjectMetadata(bucket, objectKey(stored.getId())).getVersionId();
                        // Objects created before Versioning was enabled have the literal "null" version.
                        stored.pinVersion(versionId == null ? "null" : versionId);
                        adminFileRepository.saveAndFlush(stored);
                    } catch (AmazonClientException error) {
                        log.error("관리자 파일의 기존 S3 버전 확인 실패: id={}", stored.getId(), error);
                        throw unavailable("기존 파일의 S3 버전과 읽기 권한을 확인해 주세요");
                    }
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdminFileResponseDto upload(MultipartFile file, Long uploadedBy) {
        if (file == null) throw new IllegalArgumentException("업로드할 파일을 선택해 주세요");

        String originalFilename = safeFilename(file.getOriginalFilename());
        long size = file.getSize();
        var existing = adminFileRepository.findFirstByOriginalFilenameOrderByCreatedAtDescIdDesc(originalFilename);
        AdminFile stored = existing.orElseGet(() -> AdminFile.builder().id(UUID.randomUUID().toString())
                        .originalFilename(originalFilename).size(size).uploadedBy(uploadedBy).build());
        String id = stored.getId();
        String key = objectKey(id);
        ObjectMetadata metadata = new ObjectMetadata();
        String uploadedVersionId = null;

        try {
            requirePrivateBucket();
            requireVersioningEnabled();
            if (existing.isPresent() && stored.getS3VersionId() == null) {
                throw unavailable("기존 파일의 버전 정보를 먼저 확인해 주세요");
            }

            metadata.setContentLength(size);
            metadata.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            metadata.setContentDisposition(ContentDisposition.attachment().filename(originalFilename, StandardCharsets.UTF_8).build().toString());
            metadata.setCacheControl("no-store");

            try (InputStream input = file.getInputStream()) {
                uploadedVersionId = amazonS3.putObject(new PutObjectRequest(bucket, key, input, metadata)).getVersionId();
            }
            if (uploadedVersionId == null || uploadedVersionId.isBlank() || "null".equals(uploadedVersionId)) {
                throw unavailable("새 파일의 S3 버전 ID를 확인하지 못했습니다. 버킷 버전 관리를 확인해 주세요");
            }

            stored.recordUpload(size, uploadedBy, uploadedVersionId);
            AdminFileResponseDto result = AdminFileResponseDto.from(adminFileRepository.saveAndFlush(stored));
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                String versionId = uploadedVersionId;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) removeFailedUpload(id, versionId);
                        else if (status == STATUS_UNKNOWN) log.error("관리자 파일 DB 커밋 결과 확인 필요: id={}, versionId={}", id, versionId);
                    }
                });
            }
            return result;
        } catch (IOException error) {
            removeFailedUpload(id, uploadedVersionId);
            throw new IllegalStateException("업로드 파일을 읽지 못했습니다", error);
        } catch (RuntimeException error) {
            removeFailedUpload(id, uploadedVersionId);
            throw error;
        }
    }

    public Download download(String id) {
        validateId(id);
        requirePrivateBucket();

        AdminFile metadata = adminFileRepository.findById(id).orElseThrow(this::notFound);
        GetObjectRequest request = new GetObjectRequest(bucket, objectKey(id));
        if (metadata.getS3VersionId() != null) request.setVersionId(metadata.getS3VersionId());

        try {
            S3Object object = amazonS3.getObject(request);

            return new Download(AdminFileResponseDto.from(metadata), new InputStreamResource(object.getObjectContent()));
        } catch (AmazonS3Exception error) {
            if ("NoSuchKey".equals(error.getErrorCode()) || "NoSuchVersion".equals(error.getErrorCode())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
            }
            log.error("관리자 파일 S3 다운로드 실패: id={}", id, error);
            throw unavailable("S3에서 파일을 가져오지 못했습니다");
        } catch (AmazonClientException error) {
            log.error("관리자 파일 S3 연결 실패: id={}", id, error);
            throw unavailable("S3에 연결하지 못했습니다");
        }
    }

    private void validateId(String id) {
        if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw notFound();
    }

    private String safeFilename(String filename) {
        if (filename == null) throw new IllegalArgumentException("파일 이름이 필요합니다");
        String normalized = filename.replace('\\', '/');
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\\p{Cf}]", "_").strip();
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.length() > 255) {
            throw new IllegalArgumentException("파일 이름은 1~255자로 지정해 주세요");
        }
        return name;
    }

    private String objectKey(String id) {
        return PREFIX + id;
    }

    private void requirePrivateBucket() {
        if (bucket.isBlank()) throw unavailable("관리자 파일용 비공개 S3 버킷(admin.files.s3.bucket)을 설정해 주세요");
        try {
            PublicAccessBlockConfiguration configuration = amazonS3.getPublicAccessBlock(new GetPublicAccessBlockRequest().withBucketName(bucket)).getPublicAccessBlockConfiguration();
            if (configuration == null || !Boolean.TRUE.equals(configuration.getBlockPublicAcls()) || !Boolean.TRUE.equals(configuration.getIgnorePublicAcls()) || !Boolean.TRUE.equals(configuration.getBlockPublicPolicy()) || !Boolean.TRUE.equals(configuration.getRestrictPublicBuckets())) {
                throw unavailable("관리자 파일용 S3 버킷의 퍼블릭 액세스 차단 4개 항목을 모두 켜 주세요");
            }
        } catch (AmazonClientException error) {
            log.error("관리자 S3 비공개 설정 확인 실패: bucket={}", bucket, error);
            throw unavailable("S3 비공개 설정과 GetBucketPublicAccessBlock 권한을 확인해 주세요");
        }
    }

    private void requireVersioningEnabled() {
        try {
            if (!BucketVersioningConfiguration.ENABLED.equals(amazonS3.getBucketVersioningConfiguration(bucket).getStatus())) {
                throw unavailable("관리자 파일용 S3 버킷의 버전 관리를 활성화해 주세요");
            }
        } catch (AmazonClientException error) {
            log.error("관리자 S3 버전 관리 설정 확인 실패: bucket={}", bucket, error);
            throw unavailable("S3 버전 관리 설정과 GetBucketVersioning 권한을 확인해 주세요");
        }
    }

    private void removeFailedUpload(String id, String versionId) {
        // Never delete by key: it could hide an existing file or delete another upload.
        if (versionId == null || versionId.isBlank() || "null".equals(versionId)) return;
        String key = objectKey(id);
        try {
            amazonS3.deleteVersion(bucket, key, versionId);
        } catch (AmazonClientException error) {
            log.error("실패한 관리자 S3 버전 정리 필요: bucket={}, key={}, versionId={}", bucket, key, versionId, error);
        }
    }

    private ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
    }
}
