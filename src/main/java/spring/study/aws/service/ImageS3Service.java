package spring.study.aws.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spring.study.board.entity.BoardImg;
import spring.study.chat.entity.ChatMessageImg;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageS3Service {
    private static final Set<String> IMAGE_FORMATS = Set.of("jpg", "jpeg", "png", "gif");
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    public boolean fileFormatCheck(MultipartFile file) {
        String format = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return format == null || !IMAGE_FORMATS.contains(format.toLowerCase());
    }

    public boolean findFormatCheck(List<MultipartFile> files) {
        return files.stream().anyMatch(this::fileFormatCheck);
    }

    public int fileSizeCheck(List<MultipartFile> files) {
        if (files == null || files.isEmpty())
            return -1;
        else if (files.size() > 10)
            return -2;
        else
            return files.size();
    }

    public String uploadImageToS3(MultipartFile image) throws IOException{
        String originName = image.getOriginalFilename();

        if (originName == null || originName.isBlank()) {
            throw new IOException("image original filename is empty");
        }

        String ext = StringUtils.getFilenameExtension(originName);

        if (ext == null || ext.isBlank()) {
            throw new IOException("image extension is empty");
        }

        String changedName = UUID.randomUUID() + originName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/" + ext.toLowerCase());
        metadata.setContentLength(image.getSize());

        amazonS3.putObject(new PutObjectRequest(
                bucketName, changedName, image.getInputStream(), metadata
        ));

        return amazonS3.getUrl(bucketName, changedName).toString();
    }

    public byte[] readImage(String name) throws IOException {
        try (S3Object object = amazonS3.getObject(new GetObjectRequest(bucketName, name));
             S3ObjectInputStream stream = object.getObjectContent()) {
            return IOUtils.toByteArray(stream);
        }
    }

    public void deleteImg(List<ChatMessageImg> list) {
        list.forEach(image -> deleteImage(image.getImgSrc()));
    }

    public void deleteImage(List<BoardImg> list) {
        list.forEach(image -> deleteImage(image.getImgSrc()));
    }

    public void deleteImage(String name) {
        if (name == null || name.isBlank()) return;
        String[] splitString = name.split("/");
        String fileName = splitString[splitString.length-1];

        if (amazonS3.doesObjectExist(bucketName, fileName))
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
    }

    public void deleteImgSrc(List<String> list) {
        list.forEach(this::deleteImage);
    }
}
