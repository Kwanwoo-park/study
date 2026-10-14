package spring.study.admin.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.entity.AdminFile;
import spring.study.admin.repository.AdminFileRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminFileUploadTest {
    private static final String BUCKET = "private-test-bucket";
    private static final String ID = "d0b3808b-6be3-44c1-80cf-ef639895ed9d";
    private final AdminFileRepository repository = mock(AdminFileRepository.class);
    private final AmazonS3 s3 = mock(AmazonS3.class);
    private final AdminFileService service = new AdminFileService(repository, s3);
    private final MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1, 2});

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "bucket", "private-test-bucket");
        when(s3.getPublicAccessBlock(any())).thenReturn(new GetPublicAccessBlockResult()
                .withPublicAccessBlockConfiguration(new PublicAccessBlockConfiguration()
                        .withBlockPublicAcls(true).withIgnorePublicAcls(true)
                        .withBlockPublicPolicy(true).withRestrictPublicBuckets(true)));
        when(s3.getBucketVersioningConfiguration(BUCKET)).thenReturn(new BucketVersioningConfiguration("Enabled"));
        PutObjectResult uploaded = new PutObjectResult();
        uploaded.setVersionId("new-version");
        when(s3.putObject(any(PutObjectRequest.class))).thenReturn(uploaded);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void databaseRollbackDeletesOnlyTheNewVersion(boolean replacing) {
        if (replacing) existingFile("old-version");
        TransactionSynchronizationManager.initSynchronization();
        try {
            var result = service.upload(file, 7L);
            if (replacing) assertThat(result.id()).isEqualTo(ID);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(s3).deleteVersion(BUCKET, "admin-files/" + result.id(), "new-version");
            verify(s3, never()).deleteVersion(anyString(), anyString(), eq("old-version"));
            verify(s3, never()).deleteObject(anyString(), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void matchingFilenameReusesTheUuidAndUpdatesMetadata() {
        AdminFile stored = existingFile("old-version");
        var createdAt = stored.getCreatedAt();

        assertThat(service.upload(file, 8L).id()).isEqualTo(ID);

        verify(repository).saveAndFlush(stored);
        assertThat(stored.getSize()).isEqualTo(2);
        assertThat(stored.getUploadedBy()).isEqualTo(8L);
        assertThat(stored.getS3VersionId()).isEqualTo("new-version");
        assertThat(stored.getCreatedAt()).isEqualTo(createdAt);
        var request = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture());
        assertThat(request.getValue().getKey()).isEqualTo("admin-files/" + ID);
        verify(s3, never()).deleteVersion(anyString(), anyString(), anyString());
    }

    @Test
    void immediateDatabaseFailureAlsoDeletesOnlyTheNewVersion() {
        existingFile("old-version");
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("DB failure"));

        assertThatThrownBy(() -> service.upload(file, 7L)).isInstanceOf(IllegalStateException.class);

        verify(s3).deleteVersion(BUCKET, "admin-files/" + ID, "new-version");
        verify(s3, never()).deleteObject(anyString(), anyString());
    }

    @Test
    void missingUploadResponseNeverDeletesTheExistingKey() {
        existingFile("old-version");
        when(s3.putObject(any(PutObjectRequest.class))).thenThrow(new AmazonClientException("response lost"));

        assertThatThrownBy(() -> service.upload(file, 7L)).isInstanceOf(AmazonClientException.class);

        verify(s3, never()).deleteVersion(anyString(), anyString(), anyString());
        verify(s3, never()).deleteObject(anyString(), anyString());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void suspendedVersioningIsRejectedBeforeUploading() {
        when(s3.getBucketVersioningConfiguration(BUCKET)).thenReturn(new BucketVersioningConfiguration("Suspended"));

        assertThatThrownBy(() -> service.upload(file, 7L)).isInstanceOf(ResponseStatusException.class);

        verify(s3, never()).putObject(any(PutObjectRequest.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"legacy-version"})
    void pinsLegacyVersionBeforeAnyReplacementIsUploaded(String versionId) {
        AdminFile stored = existingFile(null);
        ObjectMetadata old = new ObjectMetadata();
        old.setHeader("x-amz-version-id", versionId);
        when(s3.getObjectMetadata(BUCKET, "admin-files/" + ID)).thenReturn(old);

        service.pinExistingVersion("test.txt");

        assertThat(stored.getS3VersionId()).isEqualTo(versionId == null ? "null" : versionId);
        verify(repository).saveAndFlush(stored);
        verify(s3, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    void uncertainCommitDoesNotDeleteAPossiblyCommittedVersion() {
        existingFile("old-version");
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.upload(file, 7L);
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN));
            verify(s3, never()).deleteVersion(anyString(), anyString(), anyString());
            verify(s3, never()).deleteObject(anyString(), anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void downloadRequestsTheCommittedVersionRatherThanTheLatestS3Version() {
        AdminFile stored = existingFile("old-version");
        when(repository.findById(ID)).thenReturn(Optional.of(stored));
        S3Object object = new S3Object();
        object.setObjectContent(new java.io.ByteArrayInputStream(new byte[]{1}));
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(object);

        service.download(ID);

        var request = org.mockito.ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(request.capture());
        assertThat(request.getValue().getVersionId()).isEqualTo("old-version");
    }

    private AdminFile existingFile(String versionId) {
        AdminFile stored = new AdminFile(ID, "test.txt", 1, 7L);
        stored.pinVersion(versionId);
        when(repository.findFirstByOriginalFilenameOrderByCreatedAtDescIdDesc("test.txt"))
                .thenReturn(Optional.of(stored));
        return stored;
    }
}
