package Study.Assistant.Studia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
public class FileConfig {
    
    @Value("${file.upload.path:./uploads}")
    private String uploadPath;
    
    @PostConstruct
    public void init() {
        // Heroku에서는 /tmp 디렉터리를 사용
        if (System.getenv("DYNO") != null) {
            uploadPath = "/tmp/uploads";
        }
        
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        System.out.println("File upload path: " + uploadDir.getAbsolutePath());
    }
    
    public String getUploadPath() {
        return uploadPath;
    }
}
