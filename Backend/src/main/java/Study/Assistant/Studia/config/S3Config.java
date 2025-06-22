package Study.Assistant.Studia.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("heroku") // Heroku 환경에서만 활성화
public class S3Config {
    
    @Value("${aws.access.key.id:}")
    private String awsAccessKeyId;
    
    @Value("${aws.secret.access.key:}")
    private String awsSecretAccessKey;
    
    @Value("${aws.s3.region:us-east-1}")
    private String region;
    
    @Bean
    public AmazonS3 amazonS3() {
        if (awsAccessKeyId.isEmpty() || awsSecretAccessKey.isEmpty()) {
            // S3 설정이 없으면 로컬 파일 시스템 사용
            return null;
        }
        
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
            awsAccessKeyId, 
            awsSecretAccessKey
        );
        
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
}
