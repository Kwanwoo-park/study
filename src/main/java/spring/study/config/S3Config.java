package spring.study.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    private final String region;

    public S3Config(@Value("${cloud.aws.credentials.accessKey}") String accessKey,
                    @Value("${cloud.aws.credentials.secretKey}") String secretKey,
                    @Value("${cloud.aws.s3.bucketName}") String bucketName,
                    @Value("${cloud.aws.region.static}") String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucketName = bucketName;
        this.region = region;
    }

    @Bean
    public AmazonS3 s3Builder() {
        AWSCredentials basicAWSCredentials = new BasicAWSCredentials(accessKey, secretKey);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .withRegion(region).build();
    }
}
