package spring.study.aws.service;

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
import spring.study.board.entity.BoardImg;
import spring.study.chat.entity.ChatMessageImg;

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
        String originName = file.getOriginalFilename();
        if (originName == null || originName.isBlank()) {
            return true;
        }

        String format = StringUtils.getFilenameExtension(originName);
        if (format == null) {
            return true;
        }

        return !Arrays.stream(formatArr).toList().contains(format.toLowerCase());
    }

    public boolean findFormatCheck(List<MultipartFile> files) {
        String[] formatArr = {"jpg", "jpeg", "png", "gif"};
        String format;

        for (MultipartFile file : files) {
            String originName = file.getOriginalFilename();
            if (originName == null || originName.isBlank()) {
                return true;
            }

            format = StringUtils.getFilenameExtension(originName);
            if (format == null) {
                return true;
            }

            if (!Arrays.stream(formatArr).toList().contains(format.toLowerCase()))
                return true;
        }

        return false;
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

        String changedName = changedImageName(originName);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/" + ext.toLowerCase());
        metadata.setContentLength(image.getSize());

        amazonS3.putObject(new PutObjectRequest(
                bucketName, changedName, image.getInputStream(), metadata
        ));

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

    public void deleteImg(List<ChatMessageImg> list) {
        String[] splitString;
        String fileName;

        for (ChatMessageImg img : list) {
            splitString = img.getImgSrc().split("/");
            fileName = splitString[splitString.length-1];

            if (amazonS3.doesObjectExist(bucketName, fileName))
                amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        }
    }

    public void deleteImage(List<BoardImg> list) {
        String[] splitString;
        String fileName;

        for (BoardImg img : list) {
            splitString = img.getImgSrc().split("/");
            fileName = splitString[splitString.length-1];

            if (amazonS3.doesObjectExist(bucketName, fileName))
                amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        }
    }

    public void deleteImage(String name) {
        String[] splitString = name.split("/");
        String fileName = splitString[splitString.length-1];

        if (amazonS3.doesObjectExist(bucketName, fileName))
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
    }

    public void deleteImgSrc(List<String> list) {
        String[] splitString;
        String fileName;

        for (String imgSrc : list) {
            splitString = imgSrc.split("/");
            fileName = splitString[splitString.length-1];

            if (amazonS3.doesObjectExist(bucketName, fileName))
                amazonS3.deleteObject(new DeleteObjectRequest(bucketName, fileName));
        }
    }
}
