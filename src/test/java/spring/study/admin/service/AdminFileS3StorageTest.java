package spring.study.admin.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GetPublicAccessBlockRequest;
import com.amazonaws.services.s3.model.GetPublicAccessBlockResult;
import com.amazonaws.services.s3.model.PublicAccessBlockConfiguration;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminFileS3StorageTest {
    private static final String BUCKET = "admin-private-bucket";
    private static final String ID = "d0b3808b-6be3-44c1-80cf-ef639895ed9d";
    @Mock AmazonS3 amazonS3;
    AdminFileS3Storage storage;

    @BeforeEach
    void setUp() {
        storage = new AdminFileS3Storage(amazonS3, BUCKET);
    }

    @Test
    void uploadsStreamWithLengthAndAttachmentMetadataWithoutPublicAclOrUrl() throws IOException {
        allowPrivateBucket();
        byte[] bytes = {77, 90, 0, (byte) 255};
        try (var input = new ByteArrayInputStream(bytes)) {
            storage.upload(ID, "실행 파일.exe", bytes.length, input);
            ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(amazonS3).putObject(request.capture());
            assertThat(request.getValue().getBucketName()).isEqualTo(BUCKET);
            assertThat(request.getValue().getKey()).isEqualTo("admin-files/" + ID);
            assertThat(request.getValue().getInputStream()).isSameAs(input);
            assertThat(request.getValue().getInputStream().readAllBytes()).isEqualTo(bytes);
            assertThat(request.getValue().getFile()).isNull();
            assertThat(request.getValue().getCannedAcl()).isNull();
            assertThat(request.getValue().getAccessControlList()).isNull();
            assertThat(request.getValue().getMetadata().getContentLength()).isEqualTo(bytes.length);
            assertThat(request.getValue().getMetadata().getContentType()).isEqualTo("application/octet-stream");
            assertThat(request.getValue().getMetadata().getContentDisposition()).startsWith("attachment;");
            assertThat(request.getValue().getMetadata().getCacheControl()).isEqualTo("no-store");
            verify(amazonS3, never()).getUrl(anyString(), anyString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void refusesUploadAndDownloadUnlessAllPublicAccessBlocksAreEnabled(int missingFlag) {
        PublicAccessBlockConfiguration config = privateConfiguration();
        switch (missingFlag) {
            case 0 -> config.setBlockPublicAcls(false);
            case 1 -> config.setIgnorePublicAcls(false);
            case 2 -> config.setBlockPublicPolicy(false);
            case 3 -> config.setRestrictPublicBuckets(false);
        }
        when(amazonS3.getPublicAccessBlock(any())).thenReturn(new GetPublicAccessBlockResult().withPublicAccessBlockConfiguration(config));
        assertUnavailable(() -> storage.upload(ID, "test.exe", 0, new ByteArrayInputStream(new byte[0])));
        assertUnavailable(() -> storage.download(ID));
        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
        verify(amazonS3, never()).getObject(any(GetObjectRequest.class));
    }

    @Test
    void missingBucketDoesNotFallBackToPublicImageBucketOrLocalDisk() {
        storage = new AdminFileS3Storage(amazonS3, "");
        assertUnavailable(() -> storage.upload(ID, "test.exe", 0, new ByteArrayInputStream(new byte[0])));
        verifyNoInteractions(amazonS3);
    }

    @Test
    void failingPrivacyCheckPreventsUpload() {
        when(amazonS3.getPublicAccessBlock(any())).thenThrow(new AmazonClientException("access denied"));
        assertUnavailable(() -> storage.upload(ID, "test.exe", 0, new ByteArrayInputStream(new byte[0])));
        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    void s3UploadFailureIsReportedAsServiceUnavailable() {
        allowPrivateBucket();
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenThrow(new AmazonClientException("network unavailable"));
        assertUnavailable(() -> storage.upload(ID, "test.exe", 0, new ByteArrayInputStream(new byte[0])));
    }

    @Test
    void downloadUsesConfiguredBucketAndComputedKeyWithoutVersionAndClosesStream() throws IOException {
        allowPrivateBucket();
        AtomicBoolean closed = new AtomicBoolean();
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{0, 1, 2}) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        S3ObjectInputStream input = new S3ObjectInputStream(source, null);
        S3Object object = new S3Object();
        object.setObjectContent(input);
        when(amazonS3.getObject(any(GetObjectRequest.class))).thenReturn(object);
        var resource = storage.download(ID);
        assertThat(resource).isExactlyInstanceOf(InputStreamResource.class);
        try (var response = resource.getInputStream()) {
            assertThat(response.readAllBytes()).containsExactly(0, 1, 2);
        }
        assertThat(closed).isTrue();
        verify(amazonS3).getObject(argThat((GetObjectRequest request) -> request.getBucketName().equals(BUCKET) && request.getKey().equals("admin-files/" + ID) && request.getVersionId() == null));
        verify(amazonS3).getPublicAccessBlock(argThat((GetPublicAccessBlockRequest request) -> request.getBucketName().equals(BUCKET)));
    }

    @Test
    void missingBucketBlocksDownloadWithoutAccessingAnyOtherStorage() {
        storage = new AdminFileS3Storage(amazonS3, "");
        assertUnavailable(() -> storage.download(ID));
        verifyNoInteractions(amazonS3);
    }

    @Test
    void missingS3ObjectReturnsNotFound() {
        allowPrivateBucket();
        AmazonS3Exception error = new AmazonS3Exception("missing");
        error.setErrorCode("NoSuchKey");
        error.setStatusCode(404);
        when(amazonS3.getObject(any(GetObjectRequest.class))).thenThrow(error);
        assertThatThrownBy(() -> storage.download(ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, result -> assertThat(result.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void accessDeniedIsNotMisreportedAsMissingObject() {
        allowPrivateBucket();
        AmazonS3Exception error = new AmazonS3Exception("denied");
        error.setStatusCode(403);
        error.setErrorCode("AccessDenied");
        when(amazonS3.getObject(any(GetObjectRequest.class))).thenThrow(error);
        assertUnavailable(() -> storage.download(ID));
    }

    @Test
    void rollbackUsesConfiguredBucketAndComputedKey() {
        storage.removeFailedUpload(ID);
        verify(amazonS3).deleteObject(BUCKET, "admin-files/" + ID);
        verifyNoMoreInteractions(amazonS3);
    }

    @Test
    void rollbackDeletesUnversionedObjectAndDoesNotMaskOriginalFailure() {
        doThrow(new AmazonClientException("delete denied")).when(amazonS3).deleteObject(BUCKET, "admin-files/" + ID);
        storage.removeFailedUpload(ID);
        verify(amazonS3).deleteObject(BUCKET, "admin-files/" + ID);
    }

    private PublicAccessBlockConfiguration privateConfiguration() {
        return new PublicAccessBlockConfiguration().withBlockPublicAcls(true).withIgnorePublicAcls(true).withBlockPublicPolicy(true).withRestrictPublicBuckets(true);
    }

    private void allowPrivateBucket() {
        when(amazonS3.getPublicAccessBlock(any())).thenReturn(new GetPublicAccessBlockResult().withPublicAccessBlockConfiguration(privateConfiguration()));
    }

    private void assertUnavailable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }
}
