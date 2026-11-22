package spring.study.chat.component;

import org.junit.jupiter.api.Test;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.entity.ChatImageUpload;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.service.ChatImageUploadService;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.common.component.TransactionRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatImageUploadCleanupTest {
    @Test
    void failedCleanupKeepsTheRecordAndPreventsLaterAttachment() {
        ChatImageUploadService uploads = mock(ChatImageUploadService.class);
        ChatMessageImgService images = mock(ChatMessageImgService.class);
        ImageS3Service storage = mock(ImageS3Service.class);
        TransactionRunner transactions = mock(TransactionRunner.class);
        doAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get()).when(transactions).write(any());
        LocalDateTime time = LocalDateTime.now().minusHours(25);
        var upload = new ChatImageUpload("upload", 1L, 2L, time.plusHours(1));
        assertThat(upload.markReady(time)).isTrue();
        when(uploads.findExpiredIds()).thenReturn(List.of(upload.getId()));
        when(uploads.lock(upload.getId())).thenReturn(upload);
        String url = "https://images.example.test/staged.png";
        when(images.findMessage(upload.getId())).thenReturn(List.of(
                ChatMessageImg.builder().messageId(upload.getId()).imgSrc(url).build()));
        doThrow(new IllegalStateException("storage unavailable")).when(storage).deleteImgSrc(List.of(url));

        new ChatImageUploadCleanup(uploads, images, storage, transactions).cleanExpiredUploads();

        verify(images, never()).deleteMessage(upload.getId());
        verify(uploads, never()).delete(upload);
        assertThat(upload.canAttach(1L, 2L, LocalDateTime.now())).isFalse();
        assertThat(upload.markReady(LocalDateTime.now())).isFalse();
    }
}
