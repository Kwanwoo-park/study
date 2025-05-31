package spring.study.service.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageS3Service {
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    private String changedImageName(String originName) {
        String random = UUID.randomUUID().toString();
        return random + originName;
    }

    public boolean fileFormatCheck(MultipartFile file) {
        String[] formatArr = {"jpg", "jpeg", "png", "gif"};

        String format = StringUtils.getFilenameExtension(file.getOriginalFilename());

        return !Arrays.stream(formatArr).toList().contains(format);
    }

    public boolean findFormatCheck(List<MultipartFile> files) {
        String[] formatArr = {"jpg", "jpeg", "png", "gif"};
        String format;

        for (MultipartFile file : files) {
            format = StringUtils.getFilenameExtension(file.getOriginalFilename());

            if (!Arrays.stream(formatArr).toList().contains(format))
                return true;
        }

        return false;
    }

    public String uploadImageToS3(MultipartFile image) throws IOException{
        String originName = image.getOriginalFilename();
        String ext = originName.substring(originName.lastIndexOf("."));
        String changedName = changedImageName(originName);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/"+ext);
        metadata.setContentLength(image.getSize());

        try {
            PutObjectResult putObjectResult = amazonS3.putObject(new PutObjectRequest(
                    bucketName, changedName, image.getInputStream(), metadata
            ));
        } catch (IOException e) {
            log.debug(e.getMessage());
        }

        return amazonS3.getUrl(bucketName, changedName).toString();
    }

    public ResponseEntity<byte[]> getImage(String name, String ext) throws IOException{
        S3Object object = amazonS3.getObject(new GetObjectRequest(bucketName, name));
        S3ObjectInputStream objectInputStream = object.getObjectContent();
        byte[] bytes = IOUtils.toByteArray(objectInputStream);

        String fileName = URLEncoder.encode(name, "UTF-8").replaceAll("\\+", "%20");
        ext = ext.toLowerCase();

        HttpHeaders httpHeaders = new HttpHeaders();
        switch (ext) {
            case "jpg", "jpeg" -> httpHeaders.setContentType(MediaType.IMAGE_JPEG);
            case "gif" -> httpHeaders.setContentType(MediaType.IMAGE_GIF);
            case "png" -> httpHeaders.setContentType(MediaType.IMAGE_PNG);
        }
        httpHeaders.setContentLength(bytes.length);
        httpHeaders.setContentDispositionFormData("attachment", fileName);

        return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.OK);
    }

    public int deleteImage(String name) {
        String[] splitString = name.split("/");
        String fileName = splitString[splitString.length-1];

        if (amazonS3.doesObjectExist(bucketName, fileName))
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        else
            return 0;

        return 1;
    }
}
