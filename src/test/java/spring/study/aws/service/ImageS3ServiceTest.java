package spring.study.aws.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.board.entity.BoardImg;
import spring.study.chat.entity.ChatMessageImg;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ImageS3ServiceTest {
    private final AmazonS3 s3 = mock(AmazonS3.class);
    private final ImageS3Service service = new ImageS3Service(s3);

    @Test
    void imageValidationUsesTheSameRulesForSingleAndMultipleFiles() {
        for (String name : List.of("photo.jpg", "photo.JPEG", "photo.png", "photo.gif")) {
            MockMultipartFile file = file(name);
            assertThat(service.fileFormatCheck(file)).isFalse();
            assertThat(service.findFormatCheck(List.of(file))).isFalse();
        }
        for (String name : List.of("", "photo", "photo.", "photo.pdf")) {
            MockMultipartFile file = file(name);
            assertThat(service.fileFormatCheck(file)).isTrue();
            assertThat(service.findFormatCheck(List.of(file("valid.png"), file))).isTrue();
        }
        verifyNoInteractions(s3);
    }

    @Test
    void everyImageDeletionEntryPointResolvesTheStoredS3Key() {
        ReflectionTestUtils.setField(service, "bucketName", "test-images");
        when(s3.doesObjectExist(eq("test-images"), anyString())).thenReturn(true);

        service.deleteImage(List.of(BoardImg.builder().imgSrc("https://example.test/board.jpg").build()));
        service.deleteImg(List.of(ChatMessageImg.builder().imgSrc("https://example.test/chat.png").build()));
        service.deleteImgSrc(List.of("https://example.test/diary.gif"));

        ArgumentCaptor<DeleteObjectRequest> requests = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3, times(3)).deleteObject(requests.capture());
        assertThat(requests.getAllValues()).extracting(DeleteObjectRequest::getKey)
                .containsExactly("board.jpg", "chat.png", "diary.gif");
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "image/jpeg", new byte[]{1});
    }
}
