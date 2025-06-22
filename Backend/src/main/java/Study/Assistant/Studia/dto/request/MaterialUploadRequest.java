package Study.Assistant.Studia.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialUploadRequest {
    private MultipartFile file;
    private String title;
    private Long courseId;
    private String className;
}
