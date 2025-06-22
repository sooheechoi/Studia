package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.StudyGroup;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.repository.StudyGroupRepository;
import Study.Assistant.Studia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:5242880}") // 5MB default
    private long maxFileSize;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    /**
     * Upload profile image
     */
    public String uploadProfileImage(String userEmail, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateImageFile(file);

        String fileName = saveImageFile(file, "profile");
        String imageUrl = serverUrl + "/api/files/uploads/" + fileName;

        // Delete old profile image if exists
        if (user.getProfileImage() != null && user.getProfileImage().contains("/uploads/")) {
            deleteOldFile(user.getProfileImage());
        }

        user.setProfileImage(imageUrl);
        userRepository.save(user);

        log.info("Profile image uploaded for user: {}", userEmail);
        return imageUrl;
    }

    /**
     * Upload group image
     */
    public String uploadGroupImage(String userEmail, Long groupId, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Study group not found"));

        // Check if user is the owner
        if (!group.getOwner().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only group owner can upload group image");
        }

        validateImageFile(file);

        String fileName = saveImageFile(file, "group");
        String imageUrl = serverUrl + "/api/files/uploads/" + fileName;

        // Delete old group image if exists
        if (group.getImageUrl() != null && group.getImageUrl().contains("/uploads/")) {
            deleteOldFile(group.getImageUrl());
        }

        group.setImageUrl(imageUrl);
        studyGroupRepository.save(group);

        log.info("Group image uploaded for group: {}", groupId);
        return imageUrl;
    }

    /**
     * Load file as resource
     */
    public Resource loadFile(String filename) throws MalformedURLException {
        Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found: " + filename);
        }
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Validate actual image content
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IllegalArgumentException("Invalid image file");
            }
            
            // Optional: Check image dimensions
            if (image.getWidth() > 4096 || image.getHeight() > 4096) {
                throw new IllegalArgumentException("Image dimensions too large (max 4096x4096)");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to process image file");
        }
    }

    /**
     * Save image file
     */
    private String saveImageFile(MultipartFile file, String prefix) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String fileName = prefix + "_" + UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    /**
     * Delete old file
     */
    private void deleteOldFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadDir).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete old file: {}", e.getMessage());
        }
    }
}
