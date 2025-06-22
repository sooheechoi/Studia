package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.request.QuizAttemptRequest;
import Study.Assistant.Studia.dto.response.*;
import Study.Assistant.Studia.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Slf4j
public class QuizController {
    
    private final QuizService quizService;
    
    /**
     * 퀴즈 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<QuizResponse>> getQuizzes() {
        List<QuizResponse> quizzes = quizService.getUserQuizzes();
        return ResponseEntity.ok(quizzes);
    }
    
    /**
     * 특정 퀴즈 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuizDetailResponse> getQuiz(@PathVariable Long id) {
        QuizDetailResponse quiz = quizService.getQuizDetail(id);
        return ResponseEntity.ok(quiz);
    }
    
    /**
     * 퀴즈 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long id, Authentication authentication) {
        try {
            quizService.deleteQuiz(id);
            return ResponseEntity.ok().body(Map.of("message", "Quiz deleted successfully"));
        } catch (Exception e) {
            log.error("Failed to delete quiz", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete quiz"));
        }
    }
    
    /**
     * 퀴즈 시도 제출
     */
    @PostMapping("/{quizId}/attempts")
    public ResponseEntity<QuizAttemptResponse> submitQuizAttempt(
            @PathVariable Long quizId,
            @RequestBody QuizAttemptRequest request) {
        
        QuizAttemptResponse response = quizService.submitAttempt(quizId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 특정 학습 자료의 퀴즈 시도 기록 조회
     */
    @GetMapping("/materials/{materialId}/attempts")
    public ResponseEntity<List<QuizAttemptResponse>> getMaterialQuizAttempts(
            @PathVariable Long materialId,
            @RequestParam(value = "onlyWrong", defaultValue = "false") boolean onlyWrong) {
        
        List<QuizAttemptResponse> attempts = quizService.getMaterialAttempts(materialId, onlyWrong);
        return ResponseEntity.ok(attempts);
    }
    
    /**
     * 퀴즈 복습 데이터 조회 (정답률, 취약 문제 등)
     */
    @GetMapping("/materials/{materialId}/review")
    public ResponseEntity<QuizReviewResponse> getQuizReview(@PathVariable Long materialId) {
        QuizReviewResponse review = quizService.getQuizReview(materialId);
        return ResponseEntity.ok(review);
    }
    
    /**
     * 오답노트 조회
     */
    @GetMapping("/wrong-answers")
    public ResponseEntity<List<WrongAnswerNoteResponse>> getWrongAnswerNotes(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
        List<WrongAnswerNoteResponse> notes = quizService.getWrongAnswerNotes(courseId, limit);
        return ResponseEntity.ok(notes);
    }
    
    /**
     * 특정 시도의 상세 정보 조회
     */
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<QuizAttemptDetailResponse> getAttemptDetail(@PathVariable Long attemptId) {
        QuizAttemptDetailResponse detail = quizService.getAttemptDetail(attemptId);
        return ResponseEntity.ok(detail);
    }
    
    /**
     * 특정 학습 자료의 마지막 시도 정보 조회
     */
    @GetMapping("/materials/{materialId}/last-attempt")
    public ResponseEntity<QuizAttemptDetailResponse> getLastAttempt(@PathVariable Long materialId) {
        QuizAttemptDetailResponse detail = quizService.getLastAttemptForMaterial(materialId);
        return ResponseEntity.ok(detail);
    }
    
    /**
     * 퀴즈 시도 기록 조회 (세션 단위로 그룹화)
     */
    @GetMapping("/materials/{materialId}/history")
    public ResponseEntity<List<QuizHistoryResponse>> getQuizHistory(@PathVariable Long materialId) {
        List<QuizHistoryResponse> history = quizService.getQuizHistory(materialId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 학습 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getQuizStatistics(
            @RequestParam(value = "period", defaultValue = "WEEK") String period) {
        
        return ResponseEntity.ok(quizService.getQuizStatistics(period));
    }
}
