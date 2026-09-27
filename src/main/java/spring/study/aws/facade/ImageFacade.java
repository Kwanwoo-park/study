package spring.study.aws.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ImageFacade {
    private final ImageS3Service imageS3Service;

    public ResponseEntity<byte[]> getImage(String name, String extension) throws IOException {
        byte[] bytes = imageS3Service.readImage(name);
        HttpHeaders headers = new HttpHeaders();
        switch (extension.toLowerCase(Locale.ROOT)) {
            case "jpg", "jpeg" -> headers.setContentType(MediaType.IMAGE_JPEG);
            case "gif" -> headers.setContentType(MediaType.IMAGE_GIF);
            case "png" -> headers.setContentType(MediaType.IMAGE_PNG);
            default -> headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        headers.setContentLength(bytes.length);
        headers.setContentDispositionFormData("attachment", URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
