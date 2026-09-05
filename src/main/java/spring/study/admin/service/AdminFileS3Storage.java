package spring.study.admin.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PublicAccessBlockConfiguration;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class AdminFileS3Storage {
    private static final String PREFIX = "admin-files/";
    private final AmazonS3 amazonS3;
    private final String bucket;

    public AdminFileS3Storage(AmazonS3 amazonS3, @Value("${admin.files.s3.bucket:}") String bucket) {
        this.amazonS3 = amazonS3;
        this.bucket = bucket.strip();
    }

    public void upload(String id, String filename, long size, InputStream input) {
        requirePrivateBucket();
        String key = objectKey(id);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        metadata.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());
        metadata.setCacheControl("no-store");
        try {
            // Do not send an ACL: this also supports buckets using Bucket owner enforced.
            amazonS3.putObject(new PutObjectRequest(bucket, key, input, metadata));
        } catch (AmazonClientException error) {
            log.error("관리자 파일 S3 업로드 실패: bucket={}, key={}", bucket, key, error);
            throw unavailable("S3에 파일을 저장하지 못했습니다. 관리자에게 저장소 설정을 확인해 주세요");
        }
    }

    public Resource download(String id) {
        requirePrivateBucket();
        GetObjectRequest request = new GetObjectRequest(bucket, objectKey(id));
        try {
            S3Object object = amazonS3.getObject(request);
            // Spring closes this single-use stream after writing the attachment response.
            return new InputStreamResource(object.getObjectContent());
        } catch (AmazonS3Exception error) {
            if ("NoSuchKey".equals(error.getErrorCode())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
            }
            log.error("관리자 파일 S3 다운로드 실패: id={}", id, error);
            throw unavailable("S3에서 파일을 가져오지 못했습니다");
        } catch (AmazonClientException error) {
            log.error("관리자 파일 S3 연결 실패: id={}", id, error);
            throw unavailable("S3에 연결하지 못했습니다");
        }
    }

    public void removeFailedUpload(String id) {
        String key = objectKey(id);
        try {
            amazonS3.deleteObject(bucket, key);
        } catch (AmazonClientException error) {
            log.error("실패한 관리자 S3 업로드 정리 필요: bucket={}, key={}", bucket, key, error);
        }
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

    private ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

}
