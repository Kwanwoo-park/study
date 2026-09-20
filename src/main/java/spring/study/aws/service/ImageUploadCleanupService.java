package spring.study.aws.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadCleanupService {
    private final ImageCleanupService imageCleanupService;
    private final ImageS3Service imageS3Service;

    public boolean registerRollbackCleanup(Collection<String> uploadedUrls) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isSynchronizationActive()) return false;

        // Keep the request-local collection: uploads are added after registration.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    cleanupFailedUploads(uploadedUrls);
                } else if (status == STATUS_UNKNOWN && !uploadedUrls.isEmpty()) {
                    // Never delete potentially committed images when the DB outcome is unknown.
                    log.error("Image upload transaction outcome is unknown; reconciliation required: {}", uploadedUrls);
                }
            }
        });
        return true;
    }

    public void cleanupFailedUploads(Collection<String> uploadedUrls) {
        List<String> urls = uploadedUrls.stream().filter(url -> url != null && !url.isBlank()).distinct().toList();
        if (urls.isEmpty()) return;

        try {
            imageCleanupService.enqueueFailedUploads(urls);
        } catch (RuntimeException queueException) {
            // A DB outage must not prevent a best-effort S3 cleanup.
            log.warn("Failed to queue uploaded image cleanup; attempting S3 deletion: {}", urls, queueException);
            for (String url : urls) {
                try {
                    imageS3Service.deleteImage(url);
                } catch (RuntimeException cleanupException) {
                    log.error("Uploaded image cleanup failed; manual cleanup required: {}", url, cleanupException);
                }
            }
        }
    }
}
