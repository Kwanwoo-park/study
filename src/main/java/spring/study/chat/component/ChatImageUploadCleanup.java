package spring.study.chat.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.entity.ChatImageUpload;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.service.ChatImageUploadService;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.common.component.TransactionRunner;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatImageUploadCleanup {
    private final ChatImageUploadService uploads;
    private final ChatMessageImgService images;
    private final ImageS3Service storage;
    private final TransactionRunner transactions;

    @Scheduled(fixedDelayString = "${chat.image-upload.cleanup-delay-ms:60000}")
    public void cleanExpiredUploads() {
        for (String id : uploads.findExpiredIds()) {
            try {
                List<String> urls = transactions.write(() -> claim(id));
                if (urls == null) continue;

                storage.deleteImgSrc(urls);
                transactions.write(() -> {
                    ChatImageUpload upload = uploads.lock(id);
                    if (upload != null) {
                        images.deleteMessage(id);
                        uploads.delete(upload);
                    }
                    return null;
                });
            } catch (RuntimeException error) {
                log.warn("Chat image cleanup will retry. uploadId={}", id, error);
            }
        }
    }

    private List<String> claim(String id) {
        ChatImageUpload upload = uploads.lock(id);
        if (upload == null || !upload.claimCleanup(LocalDateTime.now())) return null;

        return images.findMessage(id).stream().map(ChatMessageImg::getImgSrc).toList();
    }
}
