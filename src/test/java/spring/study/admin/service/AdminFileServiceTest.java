package spring.study.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.dto.AdminFileResponseDto;
import spring.study.admin.entity.AdminFile;
import spring.study.admin.repository.AdminFileRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminFileServiceTest {
    @Mock AdminFileS3Storage storage;
    @Mock AdminFileRepository repository;
    @Mock PlatformTransactionManager transactionManager;
    Map<String, byte[]> uploadedBytes = new HashMap<>();
    AdminFileService service;

    @BeforeEach
    void setUp() {
        service = new AdminFileService(repository, transactionManager, storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"setup.exe", "archive.zip", "archive.tar.gz", "script.sh", "page.html", "README", "한글 파일.bin", "same-name.exe"})
    void arbitraryTypesRoundTripAsPrivateOpaqueFiles(String name) throws IOException {
        prepareSave();
        byte[] bytes = {0, 1, 2, (byte) 255};
        AdminFileResponseDto result = service.upload(new MockMultipartFile("file", name, "text/html", bytes), 7L);
        assertThat(result.originalFilename()).isEqualTo(name);
        assertThat(result.size()).isEqualTo(bytes.length);
        assertThat(result.uploadedBy()).isEqualTo(7L);
        assertThat(uploadedBytes.get(result.id())).isEqualTo(bytes);
        AdminFile metadata = new AdminFile(result.id(), name, bytes.length, 7L);
        when(repository.findById(result.id())).thenReturn(Optional.of(metadata));
        when(storage.download(result.id())).thenReturn(new ByteArrayResource(bytes));
        try (var input = service.download(result.id()).resource().getInputStream()) {
            assertThat(input.readAllBytes()).isEqualTo(bytes);
        }
        verify(repository).saveAndFlush(argThat(saved -> saved.getId().equals(result.id()) && saved.getOriginalFilename().equals(name) && saved.getSize() == bytes.length));
        verify(transactionManager).commit(any());
    }

    @Test
    void duplicateNamesNeverOverwriteAndZeroByteFilesAreAllowed() throws IOException {
        prepareSave();
        MockMultipartFile file = new MockMultipartFile("file", "empty.exe", null, new byte[0]);
        AdminFileResponseDto first = service.upload(file, 7L);
        AdminFileResponseDto second = service.upload(file, 7L);
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(uploadedBytes.get(first.id())).isEmpty();
        assertThat(uploadedBytes).hasSize(2);
    }

    @Test
    void removesClientPathsAndControlCharactersFromDownloadName() {
        prepareSave();
        AdminFileResponseDto file = service.upload(new MockMultipartFile("file", "C:\\fakepath\\..\\setup\r\n.exe", null, new byte[0]), 7L);
        assertThat(file.originalFilename()).isEqualTo("setup__.exe");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "..", ".", "../", "C:\\fakepath\\"})
    void rejectsMissingOrInvalidNames(String name) {
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", name, null, new byte[0]), 7L)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, transactionManager, storage);
    }

    @Test
    void usesMultipartSizeForS3AndDatabaseWithoutRepeatingServletValidation() throws IOException {
        prepareSave();
        byte[] bytes = new byte[8192];
        // HTTP size rejection is tested separately with real Tomcat multipart parsing.
        AdminFileResponseDto result = service.upload(new MockMultipartFile("file", "setup.exe", null, bytes), 7L);
        assertThat(result.size()).isEqualTo(bytes.length);
        assertThat(uploadedBytes.get(result.id())).isEqualTo(bytes);
        verify(storage).upload(eq(result.id()), eq("setup.exe"), eq((long) bytes.length), any());
        verify(repository).saveAndFlush(argThat(metadata -> metadata.getSize() == bytes.length));
    }

    @Test
    void unreadableMultipartStreamNeverStartsAnS3Upload() throws IOException {
        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("setup.exe");
        when(file.getSize()).thenReturn(4L);
        when(file.getInputStream()).thenThrow(new IOException("temporary file unavailable"));
        assertThatThrownBy(() -> service.upload(file, 7L)).isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(IOException.class);
        verifyNoInteractions(repository, storage, transactionManager);
    }

    @Test
    void removesFileWhenDatabaseCommitFails() throws IOException {
        prepareSave();
        doThrow(new IllegalStateException("commit failed")).when(transactionManager).commit(any());
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "setup.exe", null, new byte[10]), 7L)).isInstanceOf(IllegalStateException.class);
        verify(storage).removeFailedUpload(argThat(uploadedBytes::containsKey));
    }

    @Test
    void startsANewDatabaseTransactionAfterS3UploadAndCommitsBeforeReturning() throws IOException {
        prepareSave();
        service.upload(new MockMultipartFile("file", "setup.exe", null, new byte[4]), 7L);

        var order = inOrder(storage, transactionManager, repository);
        order.verify(storage).upload(anyString(), eq("setup.exe"), eq(4L), any());
        order.verify(transactionManager).getTransaction(argThat(definition -> definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        order.verify(repository).saveAndFlush(any(AdminFile.class));
        order.verify(transactionManager).commit(any());
    }

    @Test
    void removesFileWhenDatabaseSaveFails() throws IOException {
        prepareUpload();
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "setup.exe", null, new byte[10]), 7L)).isInstanceOf(IllegalStateException.class);
        verify(storage).removeFailedUpload(any());
        verify(transactionManager).rollback(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secret", "..\\secret", "C:\\secret", "not-a-uuid", ""})
    void downloadRejectsPathsBeforeDatabaseLookup(String id) {
        assertThatThrownBy(() -> service.download(id)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(repository);
    }

    @Test
    void missingMetadataReturnsNotFoundWithoutFetchingS3() {
        String id = UUID.randomUUID().toString();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.download(id)).isInstanceOfSatisfying(ResponseStatusException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
        verifyNoInteractions(storage);
    }

    @Test
    void s3UploadFailureNeverCreatesDatabaseMetadata() throws IOException {
        doThrow(new IllegalStateException("S3 unavailable")).when(storage).upload(anyString(), anyString(), anyLong(), any());
        assertThatThrownBy(() -> service.upload(new MockMultipartFile("file", "setup.exe", null, new byte[10]), 7L)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(repository, transactionManager);
        verify(storage, never()).removeFailedUpload(any());
    }

    @Test
    void streamCloseFailureRemovesTheUploadedObjectWithoutSavingMetadata() {
        MockMultipartFile file = new MockMultipartFile("file", "test.exe", null, new byte[4]) {
            @Override
            public java.io.InputStream getInputStream() {
                return new ByteArrayInputStream(new byte[4]) {
                    @Override
                    public void close() throws IOException {
                        throw new IOException("stream close failed");
                    }
                };
            }
        };
        assertThatThrownBy(() -> service.upload(file, 7L)).isInstanceOf(IllegalStateException.class).hasCauseInstanceOf(IOException.class);
        verify(storage).removeFailedUpload(anyString());
        verifyNoInteractions(repository, transactionManager);
    }

    @Test
    void opensAndClosesOnlyOneUploadStream() {
        prepareSave();
        AtomicInteger opened = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        MockMultipartFile file = new MockMultipartFile("file", "test.exe", null, new byte[4]) {
            @Override
            public java.io.InputStream getInputStream() {
                opened.incrementAndGet();
                return new ByteArrayInputStream(new byte[4]) {
                    @Override
                    public void close() throws IOException {
                        closed.incrementAndGet();
                        super.close();
                    }
                };
            }
        };
        service.upload(file, 7L);
        assertThat(opened).hasValue(1);
        assertThat(closed).hasValue(1);
    }

    @Test
    void listUsesBoundedPagination() {
        when(repository.findAll(any(PageRequest.class))).thenReturn(Page.empty());
        service.list(2);
        verify(repository).findAll(argThat((PageRequest request) -> request.getPageSize() == 20 && request.getPageNumber() == 2));
        assertThatThrownBy(() -> service.list(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private void prepareSave() {
        prepareUpload();
        when(repository.saveAndFlush(any(AdminFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void prepareUpload() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            java.io.InputStream input = invocation.getArgument(3);
            uploadedBytes.put(id, input.readAllBytes());
            return null;
        }).when(storage).upload(anyString(), anyString(), anyLong(), any());
    }
}
