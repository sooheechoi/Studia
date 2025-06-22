package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.request.MaterialUploadRequest;
import Study.Assistant.Studia.dto.request.MaterialUpdateRequest;
import Study.Assistant.Studia.dto.response.MaterialSummaryResponse;
import Study.Assistant.Studia.dto.response.QuizItemResponse;
import Study.Assistant.Studia.service.StudyMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class StudyMaterialController {
    
    private final StudyMaterialService studyMaterialService;
    
    /**
     * 강의 자료 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<MaterialSummaryResponse> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam("title") String title,
            @RequestParam(value = "className", required = false) String className) {
        
        MaterialSummaryResponse response = studyMaterialService.processMaterial(file, courseId, title, className);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 강의 자료 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<MaterialSummaryResponse>> getMaterials(
            @RequestParam(value = "courseId", required = false) Long courseId) {
        
        List<MaterialSummaryResponse> materials = studyMaterialService.getUserMaterials(courseId);
        return ResponseEntity.ok(materials);
    }
    
    /**
     * 특정 강의 자료 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<MaterialSummaryResponse> getMaterial(@PathVariable Long id) {
        MaterialSummaryResponse material = studyMaterialService.getMaterial(id);
        return ResponseEntity.ok(material);
    }
    
    /**
     * 퀴즈 생성
     */
    @PostMapping("/{id}/quizzes")
    public ResponseEntity<List<QuizItemResponse>> generateQuizzes(
            @PathVariable Long id,
            @RequestParam(value = "difficulty", defaultValue = "MEDIUM") String difficulty,
            @RequestParam(value = "count", defaultValue = "5") int count) {
        
        List<QuizItemResponse> quizzes = studyMaterialService.generateQuizzes(id, difficulty, count);
        return ResponseEntity.ok(quizzes);
    }
    
    /**
     * 강의 자료 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        studyMaterialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 강의 자료 업데이트
     */
    @PutMapping("/{id}")
    public ResponseEntity<MaterialSummaryResponse> updateMaterial(
            @PathVariable Long id,
            @RequestBody MaterialUpdateRequest request) {
        
        MaterialSummaryResponse response = studyMaterialService.updateMaterial(id, request.getTitle(), request.getClassName());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 요약 재생성
     */
    @PostMapping("/{id}/regenerate-summary")
    public ResponseEntity<MaterialSummaryResponse> regenerateSummary(@PathVariable Long id) {
        MaterialSummaryResponse response = studyMaterialService.regenerateSummary(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 요약 재생성 (새 엔드포인트)
     */
    @PostMapping("/{id}/summary")
    public ResponseEntity<MaterialSummaryResponse> generateSummary(@PathVariable Long id) {
        MaterialSummaryResponse response = studyMaterialService.regenerateSummary(id);
        return ResponseEntity.ok(response);
    }
}
