package spring.study.regression;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import spring.study.aws.service.ImageS3Service;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageS3ServiceRegressionTest {

    @Mock
    private AmazonS3 amazonS3;

    private ImageS3Service imageS3Service;

    @BeforeEach
    void setUp() {
        imageS3Service = new ImageS3Service(amazonS3);
        ReflectionTestUtils.setField(imageS3Service, "bucketName", "test-bucket");
    }

    @Test
    void fileFormatCheckShouldRejectNullOrMissingExtensionAndAllowUppercase() {
        MockMultipartFile noName = new MockMultipartFile("file", "", "image/png", "a".getBytes());
        MockMultipartFile noExt = new MockMultipartFile("file", "photo", "image/png", "a".getBytes());
        MockMultipartFile upper = new MockMultipartFile("file", "photo.PNG", "image/png", "a".getBytes());

        assertTrue(imageS3Service.fileFormatCheck(noName));
        assertTrue(imageS3Service.fileFormatCheck(noExt));
        assertFalse(imageS3Service.fileFormatCheck(upper));
    }

    @Test
    void uploadImageToS3ShouldThrowWhenFilenameOrExtensionIsInvalid() {
        MockMultipartFile noName = new MockMultipartFile("file", "", "image/png", "a".getBytes());
        MockMultipartFile noExt = new MockMultipartFile("file", "photo", "image/png", "a".getBytes());

        assertThrows(IOException.class, () -> imageS3Service.uploadImageToS3(noName));
        assertThrows(IOException.class, () -> imageS3Service.uploadImageToS3(noExt));
    }

    @Test
    void uploadImageToS3ShouldUseNormalizedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.PNG", "image/png", "a".getBytes());
        when(amazonS3.getUrl(eq("test-bucket"), anyString())).thenReturn(new java.net.URL("https://example.com/a"));

        String result = imageS3Service.uploadImageToS3(file);

        assertEquals("https://example.com/a", result);

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(captor.capture());
        assertEquals("image/png", captor.getValue().getMetadata().getContentType());
    }
}
