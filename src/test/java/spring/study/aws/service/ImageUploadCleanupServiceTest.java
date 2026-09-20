package spring.study.aws.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class ImageUploadCleanupServiceTest {
    private final ImageCleanupService imageCleanupService = mock(ImageCleanupService.class);
    private final ImageS3Service imageS3Service = mock(ImageS3Service.class);
    private final ImageUploadCleanupService service = new ImageUploadCleanupService(imageCleanupService, imageS3Service);

    @AfterEach
    void clearTransactionState() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void noTransactionLeavesCleanupToTheCaller() {
        assertThat(service.registerRollbackCleanup(new ArrayList<>())).isFalse();
        verifyNoInteractions(imageCleanupService, imageS3Service);
    }

    @Test
    void unknownOutcomeNeverDeletesPossiblyCommittedImages() {
        register(List.of("new.png")).afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
        verifyNoInteractions(imageCleanupService, imageS3Service);
    }

    @Test
    void emptyUploadDoesNotCreateCleanupTasks() {
        service.cleanupFailedUploads(List.of());
        verifyNoInteractions(imageCleanupService, imageS3Service);
    }

    @Test
    void queueFailureFallsBackToS3AndContinuesAfterIndividualDeleteFailures() {
        List<String> urls = List.of("first.png", "second.png");
        doThrow(new IllegalStateException("DB unavailable")).when(imageCleanupService).enqueueFailedUploads(urls);
        doThrow(new IllegalStateException("S3 unavailable")).when(imageS3Service).deleteImage("first.png");

        assertThatCode(() -> service.cleanupFailedUploads(urls)).doesNotThrowAnyException();

        verify(imageS3Service).deleteImage("first.png");
        verify(imageS3Service).deleteImage("second.png");
    }

    private TransactionSynchronization register(List<String> urls) {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        assertThat(service.registerRollbackCleanup(urls)).isTrue();
        return TransactionSynchronizationManager.getSynchronizations().get(0);
    }
}
