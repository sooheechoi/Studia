package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.Quiz;
import Study.Assistant.Studia.domain.entity.StudyMaterial;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.dto.response.MaterialSummaryResponse;
import Study.Assistant.Studia.dto.response.QuizItemResponse;
import Study.Assistant.Studia.repository.StudyMaterialRepository;
import Study.Assistant.Studia.repository.QuizRepository;
import Study.Assistant.Studia.repository.UserRepository;
import Study.Assistant.Studia.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudyMaterialService {
    
    private final StudyMaterialRepository studyMaterialRepository;
    private final QuizRepository quizRepository;
    private final FileProcessingService fileProcessingService;
    private final AIService aiService;
    private final UserRepository userRepository;
    private final InputSanitizer inputSanitizer;
    
    public MaterialSummaryResponse processMaterial(MultipartFile file, Long courseId, String title, String className) {
        try {
            // 입력 검증
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is empty or not provided");
            }
            
            // 파일 타입 검증
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.contains("pdf") && 
                !contentType.contains("text") && 
                !contentType.contains("document") &&
                !contentType.contains("sheet"))) {
                throw new IllegalArgumentException("Unsupported file type: " + contentType);
            }
            
            String sanitizedTitle = inputSanitizer.sanitizeHtml(title);
            if (sanitizedTitle == null || sanitizedTitle.trim().isEmpty()) {
                sanitizedTitle = inputSanitizer.sanitizeFileName(file.getOriginalFilename());
            }
            
            String sanitizedClassName = className != null ? inputSanitizer.sanitizeHtml(className) : null;
            
            // 파일 크기 검증 (100MB)
            if (file.getSize() > 100 * 1024 * 1024) {
                throw new IllegalArgumentException("File size exceeds 100MB limit");
            }
            
            log.info("Processing file: {} ({}), size: {} bytes", 
                    file.getOriginalFilename(), contentType, file.getSize());
            
            // 1. 파일에서 텍스트 추출
            String content = fileProcessingService.extractTextFromFile(file);
            
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("Failed to extract content from file");
            }
            
            // 2. 현재 로그인한 사용자 정보 가져오기
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated()) {
                throw new RuntimeException("User not authenticated");
            }
            
            String email = auth.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            
            // 3. StudyMaterial 엔티티 생성 및 저장
            StudyMaterial material = StudyMaterial.builder()
                    .title(sanitizedTitle)
                    .originalFileName(inputSanitizer.sanitizeFileName(file.getOriginalFilename()))
                    .storedFileName(java.util.UUID.randomUUID().toString() + "_" + inputSanitizer.sanitizeFileName(file.getOriginalFilename()))
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .rawContent(content)
                    .className(sanitizedClassName)
                    .status(StudyMaterial.ProcessingStatus.PROCESSING)
                    .user(user)
                    .build();
            
            material = studyMaterialRepository.save(material);
            log.info("Material saved with ID: {}", material.getId());
            
            try {
                // 3. AI를 사용하여 요약 생성
                String summary = aiService.generateSummary(content);
                material.setSummary(summary);
                
                // 4. 핵심 포인트 추출
                List<String> keyPoints = aiService.extractKeyPoints(content);
                material.setKeyPoints(String.join("\n", keyPoints));
                material.setStatus(StudyMaterial.ProcessingStatus.COMPLETED);
                material.setProcessedAt(LocalDateTime.now());
                
                material = studyMaterialRepository.save(material);
                log.info("Material processing completed for ID: {}", material.getId());
                
            } catch (Exception aiError) {
                log.error("AI processing failed for material ID: {}", material.getId(), aiError);
                material.setStatus(StudyMaterial.ProcessingStatus.FAILED);
                material.setSummary("AI processing failed. Please try regenerating the summary later.");
                material = studyMaterialRepository.save(material);
            }
            
            return convertToResponse(material);
            
        } catch (IOException e) {
            log.error("Error processing file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid input: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during material processing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process material: " + e.getMessage(), e);
        }
    }
    
    public List<MaterialSummaryResponse> getUserMaterials(Long courseId) {
        // 현재 로그인한 사용자 정보 가져오기
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Long userId = user.getId();
        
        List<StudyMaterial> materials;
        if (courseId != null) {
            materials = studyMaterialRepository.findByUserIdAndCourseId(userId, courseId);
        } else {
            materials = studyMaterialRepository.findByUserId(userId);
        }
        
        return materials.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public MaterialSummaryResponse getMaterial(Long id) {
        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        return convertToResponse(material);
    }
    
    public List<QuizItemResponse> generateQuizzes(Long materialId, String difficulty, int count) {
        // 퀴즈 개수 검증 (5-50개)
        if (count < 5 || count > 50) {
            throw new IllegalArgumentException("퀴즈 개수는 5개에서 50개 사이여야 합니다.");
        }
        
        StudyMaterial material = studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        
        // AI를 사용하여 퀴즈 생성
        List<Map<String, Object>> quizData = aiService.generateQuizzes(
                material.getRawContent(), 
                count, 
                difficulty
        );
        
        List<Quiz> quizzes = new ArrayList<>();
        for (Map<String, Object> data : quizData) {
            Quiz quiz = Quiz.builder()
                    .studyMaterial(material)
                    .question((String) data.get("question"))
                    .questionType(Quiz.QuestionType.MULTIPLE_CHOICE)
                    .difficulty(Quiz.Difficulty.valueOf(difficulty.toUpperCase()))
                    .correctAnswer((String) data.get("correctAnswer"))
                    .explanation((String) data.get("explanation"))
                    .className(material.getClassName()) // className 추가
                    .build();
            
            // 선택지 설정
            if (data.get("options") instanceof List) {
                quiz.setOptions((List<String>) data.get("options"));
            }
            
            // 카테고리와 힌트 추가
            if (data.containsKey("category")) {
                quiz.setCategory((String) data.get("category"));
            }
            if (data.containsKey("hint")) {
                quiz.setHint((String) data.get("hint"));
            }
            
            quizzes.add(quiz);
        }
        
        // 생성된 퀴즈 저장
        quizzes = quizRepository.saveAll(quizzes);
        
        return quizzes.stream()
                .map(this::convertToQuizResponse)
                .collect(Collectors.toList());
    }
    
    public MaterialSummaryResponse updateMaterial(Long id, String title, String className) {
        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        
        // 현재 로그인한 사용자 확인
        org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 권한 확인
        if (!material.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to update this material");
        }
        
        // 업데이트
        if (title != null && !title.trim().isEmpty()) {
            material.setTitle(inputSanitizer.sanitizeHtml(title));
        }
        
        material.setClassName(className != null ? inputSanitizer.sanitizeHtml(className) : null);
        
        material = studyMaterialRepository.save(material);
        
        // 관련된 퀴즈들의 className도 업데이트
        List<Quiz> quizzes = quizRepository.findByStudyMaterialId(material.getId());
        for (Quiz quiz : quizzes) {
            quiz.setClassName(material.getClassName());
        }
        quizRepository.saveAll(quizzes);
        
        return convertToResponse(material);
    }
    
    public void deleteMaterial(Long id) {
        studyMaterialRepository.deleteById(id);
    }
    
    public MaterialSummaryResponse regenerateSummary(Long id) {
        StudyMaterial material = studyMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Material not found"));
        
        // 재생성 전 상태를 PROCESSING으로 변경
        material.setStatus(StudyMaterial.ProcessingStatus.PROCESSING);
        studyMaterialRepository.save(material);
        
        try {
            // AI를 사용하여 요약 재생성
            String summary = aiService.generateSummary(material.getRawContent());
            material.setSummary(summary);
            
            // 핵심 포인트 재추출
            List<String> keyPoints = aiService.extractKeyPoints(material.getRawContent());
            material.setKeyPoints(String.join("\n", keyPoints));
            
            material.setStatus(StudyMaterial.ProcessingStatus.COMPLETED);
            material.setProcessedAt(LocalDateTime.now());
            
            material = studyMaterialRepository.save(material);
            
            return convertToResponse(material);
        } catch (Exception e) {
            log.error("Error regenerating summary for material {}: {}", id, e.getMessage());
            material.setStatus(StudyMaterial.ProcessingStatus.FAILED);
            studyMaterialRepository.save(material);
            throw new RuntimeException("Failed to regenerate summary", e);
        }
    }
    
    private MaterialSummaryResponse convertToResponse(StudyMaterial material) {
        return MaterialSummaryResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .originalFileName(material.getOriginalFileName())
                .fileType(material.getFileType())
                .fileSize(material.getFileSize())
                .summary(material.getSummary())
                .keyPoints(material.getKeyPoints())
                .status(material.getStatus().toString())
                .courseId(material.getCourse() != null ? material.getCourse().getId() : null)
                .courseName(material.getCourse() != null ? material.getCourse().getCourseName() : null)
                .className(material.getClassName())
                .createdAt(material.getCreatedAt())
                .processedAt(material.getProcessedAt())
                .build();
    }
    
    private QuizItemResponse convertToQuizResponse(Quiz quiz) {
        return QuizItemResponse.builder()
                .id(quiz.getId())
                .question(quiz.getQuestion())
                .questionType(quiz.getQuestionType().toString())
                .difficulty(quiz.getDifficulty().toString())
                .options(quiz.getOptions())
                .correctAnswer(quiz.getCorrectAnswer())
                .explanation(quiz.getExplanation())
                .category(quiz.getCategory())
                .hint(quiz.getHint())
                .build();
    }
}
