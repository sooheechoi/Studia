package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.*;
import Study.Assistant.Studia.dto.request.QuizAttemptRequest;
import Study.Assistant.Studia.dto.response.*;
import Study.Assistant.Studia.repository.QuizAttemptRepository;
import Study.Assistant.Studia.repository.QuizRepository;
import Study.Assistant.Studia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuizService {
    
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final UserRepository userRepository;
    
    /**
     * 사용자의 퀴즈 목록 조회
     */
    public List<QuizResponse> getUserQuizzes() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Quiz> quizzes = quizRepository.findByStudyMaterial_UserId(user.getId());
        
        // 퀴즈별로 그룹화하여 StudyMaterial별 퀴즈 그룹 생성
        Map<Long, List<Quiz>> quizzesByMaterial = quizzes.stream()
                .collect(Collectors.groupingBy(q -> q.getStudyMaterial().getId()));
        
        return quizzesByMaterial.entrySet().stream()
                .map(entry -> {
                    StudyMaterial material = quizzes.stream()
                            .filter(q -> q.getStudyMaterial().getId().equals(entry.getKey()))
                            .findFirst()
                            .map(Quiz::getStudyMaterial)
                            .orElse(null);
                    
                    if (material == null) return null;
                    
                    // 해당 material의 모든 시도 가져오기
                    List<QuizAttempt> materialAttempts = quizAttemptRepository
                            .findByUserIdAndQuiz_StudyMaterial_Id(user.getId(), material.getId());
                    
                    // 시도를 세션별로 그룹화 (1분 이내의 시도들은 같은 세션으로 간주)
                    List<List<QuizAttempt>> attemptSessions = groupAttemptsBySession(materialAttempts);
                    int totalAttempts = attemptSessions.size();
                    
                    // 최고 점수 계산
                    Double bestScore = 0.0;
                    if (!attemptSessions.isEmpty()) {
                        bestScore = attemptSessions.stream()
                                .mapToDouble(session -> {
                                    // 같은 세션의 중복 문제 제거
                                    Set<Long> uniqueQuestions = session.stream()
                                            .map(a -> a.getQuiz().getId())
                                            .collect(Collectors.toSet());
                                    
                                    long correct = session.stream()
                                            .filter(a -> uniqueQuestions.contains(a.getQuiz().getId()))
                                            .collect(Collectors.groupingBy(a -> a.getQuiz().getId()))
                                            .values().stream()
                                            .map(list -> list.stream().filter(QuizAttempt::getIsCorrect).findFirst().isPresent() ? 1L : 0L)
                                            .reduce(0L, Long::sum);
                                    
                                    int totalQuestions = entry.getValue().size();
                                    if (totalQuestions == 0) return 0.0;
                                    
                                    double percentage = (double) correct / totalQuestions * 100;
                                    return Math.min(percentage, 100.0); // 100%를 넘지 않도록 제한
                                })
                                .max()
                                .orElse(0.0);
                    }
                    
                    return QuizResponse.builder()
                            .id(entry.getKey()) // Material ID as quiz group ID
                            .title(material.getTitle() + " Quiz")
                            .material(MaterialSummaryResponse.builder()
                                    .id(material.getId())
                                    .title(material.getTitle())
                                    .originalFileName(material.getOriginalFileName())
                                    .className(material.getClassName())
                                    .build())
                            .questionCount(entry.getValue().size())
                            .attempts(totalAttempts)
                            .totalAttempts(materialAttempts.size()) // 실제 DB 레코드 수
                            .bestScore(Math.round(bestScore * 100.0) / 100.0) // 소수점 2자리
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 시도들을 세션별로 그룹화 (1분 이내의 시도들은 같은 세션으로 간주)
     */
    private List<List<QuizAttempt>> groupAttemptsBySession(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return new ArrayList<>();
        
        // 시간순 정렬
        List<QuizAttempt> sortedAttempts = attempts.stream()
                .sorted((a, b) -> a.getAttemptedAt().compareTo(b.getAttemptedAt()))
                .collect(Collectors.toList());
        
        List<List<QuizAttempt>> sessions = new ArrayList<>();
        List<QuizAttempt> currentSession = new ArrayList<>();
        LocalDateTime lastTime = null;
        
        for (QuizAttempt attempt : sortedAttempts) {
            if (lastTime == null || 
                ChronoUnit.MINUTES.between(lastTime, attempt.getAttemptedAt()) > 1) {
                // 새로운 세션 시작
                if (!currentSession.isEmpty()) {
                    sessions.add(new ArrayList<>(currentSession));
                }
                currentSession = new ArrayList<>();
            }
            currentSession.add(attempt);
            lastTime = attempt.getAttemptedAt();
        }
        
        // 마지막 세션 추가
        if (!currentSession.isEmpty()) {
            sessions.add(currentSession);
        }
        
        return sessions;
    }
    
    /**
     * 특정 퀴즈 상세 조회
     */
    public QuizDetailResponse getQuizDetail(Long materialId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Quiz> quizzes = quizRepository.findByStudyMaterial_IdAndStudyMaterial_UserId(materialId, user.getId());
        
        if (quizzes.isEmpty()) {
            throw new RuntimeException("Quiz not found");
        }
        
        StudyMaterial material = quizzes.get(0).getStudyMaterial();
        
        List<QuizDetailResponse.QuestionDetail> questions = quizzes.stream()
                .map(quiz -> QuizDetailResponse.QuestionDetail.builder()
                        .id(quiz.getId())
                        .questionText(quiz.getQuestion())
                        .questionType(quiz.getQuestionType().toString())
                        .difficulty(quiz.getDifficulty().toString())
                        .options(quiz.getOptions())
                        .correctOption(quiz.getOptions().indexOf(quiz.getCorrectAnswer()))
                        .explanation(quiz.getExplanation())
                        .hint(quiz.getHint())
                        .category(quiz.getCategory())
                        .build())
                .collect(Collectors.toList());
        
        return QuizDetailResponse.builder()
                .id(materialId)
                .title(material.getTitle() + " Quiz")
                .questions(questions)
                .totalQuestions(questions.size())
                .estimatedTime(questions.size() * 2) // 2 minutes per question estimate
                .build();
    }
    
    /**
     * 퀴즈 삭제
     */
    @Transactional
    public void deleteQuiz(Long materialId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Quiz> quizzes = quizRepository.findByStudyMaterial_IdAndStudyMaterial_UserId(materialId, user.getId());
        
        if (quizzes.isEmpty()) {
            throw new RuntimeException("No quizzes found for this material");
        }
        
        // Delete all quizzes for the material
        quizRepository.deleteAll(quizzes);
        log.info("Deleted {} quizzes for material ID: {}", quizzes.size(), materialId);
    }
    
    /**
     * 퀴즈 시도 제출 및 채점
     */
    public QuizAttemptResponse submitAttempt(Long materialId, QuizAttemptRequest request) {
        // 사용자 정보 가져오기
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 해당 material의 모든 퀴즈 가져오기
        List<Quiz> quizzes = quizRepository.findByStudyMaterial_IdAndStudyMaterial_UserId(materialId, user.getId());
        
        if (quizzes.isEmpty()) {
            throw new RuntimeException("No quizzes found for this material");
        }
        
        int totalScore = 0;
        int correctCount = 0;
        List<QuizAttempt> attempts = new ArrayList<>();
        
        // 시작 시간 설정 (전달받지 않은 경우 현재 시간에서 totalTimeSpent를 뺀 시간)
        LocalDateTime attemptTime = request.getStartedAt() != null ? 
                request.getStartedAt() : 
                LocalDateTime.now().minusSeconds(request.getTotalTimeSpent() != null ? request.getTotalTimeSpent() : 0);
        
        // 전체 소요 시간 (초 단위)
        int totalDuration = request.getTotalTimeSpent() != null ? request.getTotalTimeSpent().intValue() : 0;
        
        // 각 답변 처리
        for (QuizAttemptRequest.Answer answer : request.getAnswers()) {
            Quiz quiz = quizzes.stream()
                    .filter(q -> q.getId().equals(answer.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Quiz not found: " + answer.getQuestionId()));
            
            String userAnswer = answer.getSelectedOption() >= 0 && answer.getSelectedOption() < quiz.getOptions().size() 
                    ? quiz.getOptions().get(answer.getSelectedOption()) 
                    : "";
            
            boolean isCorrect = quiz.getCorrectAnswer().equals(userAnswer);
            int score = calculateScore(quiz.getDifficulty(), isCorrect);
            
            if (isCorrect) {
                correctCount++;
            }
            totalScore += score;
            
            // 시도 기록 저장 (duration 포함)
            QuizAttempt attempt = QuizAttempt.builder()
                    .quiz(quiz)
                    .user(user)
                    .userAnswer(userAnswer)
                    .isCorrect(isCorrect)
                    .score(score)
                    .attemptedAt(attemptTime)
                    .duration(totalDuration) // 전체 소요 시간 저장
                    .build();
            
            attempts.add(attempt);
        }
        
        // 모든 시도 저장
        quizAttemptRepository.saveAll(attempts);
        
        // 전체 결과 반환
        return QuizAttemptResponse.builder()
                .score(correctCount)
                .total(quizzes.size())
                .percentage((double) correctCount / quizzes.size() * 100)
                .build();
    }
    
    /**
     * 특정 학습 자료의 퀴즈 시도 기록 조회
     */
    public List<QuizAttemptResponse> getMaterialAttempts(Long materialId, boolean onlyWrong) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<QuizAttempt> attempts;
        if (onlyWrong) {
            attempts = quizAttemptRepository.findByUserIdAndQuiz_StudyMaterial_IdAndIsCorrectFalse(
                    user.getId(), materialId);
        } else {
            attempts = quizAttemptRepository.findByUserIdAndQuiz_StudyMaterial_Id(
                    user.getId(), materialId);
        }
        
        return attempts.stream()
                .map(this::convertToAttemptResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 퀴즈 복습 데이터 생성
     */
    public QuizReviewResponse getQuizReview(Long materialId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 해당 자료의 모든 퀴즈 시도 가져오기
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserIdAndQuiz_StudyMaterial_Id(
                user.getId(), materialId);
        
        // 퀴즈별 시도 횟수 및 정답률 계산
        Map<Long, List<QuizAttempt>> attemptsByQuiz = attempts.stream()
                .collect(Collectors.groupingBy(a -> a.getQuiz().getId()));
        
        int totalAttempts = attempts.size();
        int correctAttempts = (int) attempts.stream().filter(QuizAttempt::getIsCorrect).count();
        double overallAccuracy = totalAttempts > 0 ? 
                (double) correctAttempts / totalAttempts * 100 : 0;
        
        List<QuizReviewResponse.QuizStats> quizStatsList = attemptsByQuiz.entrySet().stream()
                .map(entry -> {
                    Long quizId = entry.getKey();
                    List<QuizAttempt> quizAttempts = entry.getValue();
                    Quiz quiz = quizAttempts.get(0).getQuiz();
                    
                    int quizTotalAttempts = quizAttempts.size();
                    int quizCorrectAttempts = (int) quizAttempts.stream()
                            .filter(QuizAttempt::getIsCorrect).count();
                    double accuracy = (double) quizCorrectAttempts / quizTotalAttempts * 100;
                    
                    return QuizReviewResponse.QuizStats.builder()
                            .quizId(quizId)
                            .question(quiz.getQuestion())
                            .difficulty(quiz.getDifficulty().toString())
                            .totalAttempts(quizTotalAttempts)
                            .correctAttempts(quizCorrectAttempts)
                            .accuracy(accuracy)
                            .lastAttemptedAt(quizAttempts.stream()
                                    .map(QuizAttempt::getAttemptedAt)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(null))
                            .build();
                })
                .collect(Collectors.toList());
        
        // 취약 문제 (정답률 50% 미만) 식별
        List<Long> weakQuizIds = quizStatsList.stream()
                .filter(stats -> stats.getAccuracy() < 50)
                .map(QuizReviewResponse.QuizStats::getQuizId)
                .collect(Collectors.toList());
        
        return QuizReviewResponse.builder()
                .materialId(materialId)
                .totalAttempts(totalAttempts)
                .correctAttempts(correctAttempts)
                .overallAccuracy(overallAccuracy)
                .quizStatsList(quizStatsList)
                .weakQuizIds(weakQuizIds)
                .build();
    }
    
    /**
     * 오답노트 조회
     */
    public List<WrongAnswerNoteResponse> getWrongAnswerNotes(Long courseId, int limit) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<QuizAttempt> wrongAttempts;
        if (courseId != null) {
            wrongAttempts = quizAttemptRepository.findWrongAttemptsByCourse(
                    user.getId(), courseId, limit);
        } else {
            wrongAttempts = quizAttemptRepository.findRecentWrongAttempts(
                    user.getId(), limit);
        }
        
        return wrongAttempts.stream()
                .map(this::convertToWrongAnswerNote)
                .collect(Collectors.toList());
    }
    
    /**
     * 특정 시도의 상세 정보 조회
     */
    public QuizAttemptDetailResponse getAttemptDetail(Long attemptId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 해당 시도 ID로 모든 QuizAttempt 가져오기
        // (같은 시간에 제출된 같은 material의 모든 문제 시도)
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(user.getId());
        
        // attemptId와 같은 시간대의 시도들 찾기
        QuizAttempt targetAttempt = attempts.stream()
                .filter(a -> a.getId().equals(attemptId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        // 같은 material의 같은 시간대 시도들 모두 가져오기
        LocalDateTime attemptTime = targetAttempt.getAttemptedAt();
        Long materialId = targetAttempt.getQuiz().getStudyMaterial().getId();
        
        List<QuizAttempt> materialAttempts = attempts.stream()
                .filter(a -> a.getQuiz().getStudyMaterial().getId().equals(materialId))
                .filter(a -> Math.abs(ChronoUnit.SECONDS.between(a.getAttemptedAt(), attemptTime)) < 60) // 1분 이내
                .collect(Collectors.toList());
        
        return buildAttemptDetailResponse(materialAttempts);
    }
    
    /**
     * 특정 학습 자료의 마지막 시도 정보 조회
     */
    public QuizAttemptDetailResponse getLastAttemptForMaterial(Long materialId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 해당 material의 모든 시도 가져오기
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByUserIdAndQuiz_StudyMaterial_Id(user.getId(), materialId);
        
        if (attempts.isEmpty()) {
            throw new RuntimeException("No attempts found for this material");
        }
        
        // 가장 최근 시도 시간 찾기
        LocalDateTime lastAttemptTime = attempts.stream()
                .map(QuizAttempt::getAttemptedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        
        // 해당 시간대의 모든 시도 가져오기 (같은 세션의 시도들)
        List<QuizAttempt> lastSessionAttempts = attempts.stream()
                .filter(a -> Math.abs(ChronoUnit.SECONDS.between(a.getAttemptedAt(), lastAttemptTime)) < 60) // 1분 이내
                .collect(Collectors.toList());
        
        return buildAttemptDetailResponse(lastSessionAttempts);
    }
    
    /**
     * QuizAttemptDetailResponse 빌드
     */
    private QuizAttemptDetailResponse buildAttemptDetailResponse(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) {
            throw new RuntimeException("No attempts found");
        }
        
        StudyMaterial material = attempts.get(0).getQuiz().getStudyMaterial();
        LocalDateTime attemptTime = attempts.get(0).getAttemptedAt();
        
        // 각 문제별 시도 정보 생성
        List<QuizAttemptDetailResponse.QuestionAttemptDetail> questionDetails = attempts.stream()
                .map(attempt -> {
                    Quiz quiz = attempt.getQuiz();
                    int userSelectedOption = -1;
                    
                    // 사용자가 선택한 옵션의 인덱스 찾기
                    if (attempt.getUserAnswer() != null && !attempt.getUserAnswer().isEmpty()) {
                        userSelectedOption = quiz.getOptions().indexOf(attempt.getUserAnswer());
                    }
                    
                    return QuizAttemptDetailResponse.QuestionAttemptDetail.builder()
                            .questionId(quiz.getId())
                            .questionText(quiz.getQuestion())
                            .questionType(quiz.getQuestionType().toString())
                            .difficulty(quiz.getDifficulty().toString())
                            .options(quiz.getOptions())
                            .correctOption(quiz.getOptions().indexOf(quiz.getCorrectAnswer()))
                            .userSelectedOption(userSelectedOption)
                            .userAnswer(attempt.getUserAnswer())
                            .correctAnswer(quiz.getCorrectAnswer())
                            .isCorrect(attempt.getIsCorrect())
                            .explanation(quiz.getExplanation())
                            .hint(quiz.getHint())
                            .category(quiz.getCategory())
                            .build();
                })
                .collect(Collectors.toList());
        
        int score = (int) attempts.stream().filter(QuizAttempt::getIsCorrect).count();
        int total = attempts.size();
        
        // 실제 소요 시간 계산
        // 첫 번째 시도에 저장된 duration 사용
        int duration = attempts.get(0).getDuration() != null ? attempts.get(0).getDuration() : 0;
        
        // duration이 없으면 추정 (문제당 120초)
        if (duration == 0) {
            duration = total * 120; // 문제당 2분 추정
        }
        
        return QuizAttemptDetailResponse.builder()
                .attemptId(attempts.get(0).getId())
                .materialId(material.getId())
                .materialTitle(material.getTitle())
                .attemptedAt(attemptTime)
                .duration(duration)
                .score(score)
                .totalQuestions(total)
                .percentage((double) score / total * 100)
                .questionAttempts(questionDetails)
                .build();
    }
    
    /**
     * 퀴즈 히스토리 조회 (세션 단위로 그룹화)
     */
    public List<QuizHistoryResponse> getQuizHistory(Long materialId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 해당 material의 모든 시도 가져오기
        List<QuizAttempt> attempts = quizAttemptRepository
                .findByUserIdAndQuiz_StudyMaterial_Id(user.getId(), materialId);
        
        // 시도를 세션별로 그룹화
        List<List<QuizAttempt>> sessions = groupAttemptsBySession(attempts);
        
        // 각 세션을 QuizHistoryResponse로 변환
        return sessions.stream()
                .map(session -> {
                    int score = (int) session.stream().filter(QuizAttempt::getIsCorrect).count();
                    int total = session.size();
                    double percentage = (double) score / total * 100;
                    
                    // 세션의 첫 번째 시도에서 정보 가져오기
                    QuizAttempt firstAttempt = session.get(0);
                    
                    return QuizHistoryResponse.builder()
                            .attemptId(firstAttempt.getId())
                            .attemptedAt(firstAttempt.getAttemptedAt())
                            .score(score)
                            .totalQuestions(total)
                            .percentage(percentage)
                            .duration(firstAttempt.getDuration() != null ? firstAttempt.getDuration() : 0)
                            .materialTitle(firstAttempt.getQuiz().getStudyMaterial().getTitle())
                            .build();
                })
                .sorted((a, b) -> b.getAttemptedAt().compareTo(a.getAttemptedAt())) // 최신순 정렬
                .collect(Collectors.toList());
    }
    
    /**
     * 학습 통계 생성
     */
    public Map<String, Object> getQuizStatistics(String period) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocalDateTime startDate = calculateStartDate(period);
        
        // 기간 내 전체 통계
        List<QuizAttempt> periodAttempts = quizAttemptRepository
                .findByUserIdAndAttemptedAtAfter(user.getId(), startDate);
        
        int totalAttempts = periodAttempts.size();
        int correctCount = (int) periodAttempts.stream()
                .filter(QuizAttempt::getIsCorrect).count();
        double accuracy = totalAttempts > 0 ? 
                (double) correctCount / totalAttempts * 100 : 0;
        
        // 난이도별 통계
        Map<String, Double> accuracyByDifficulty = periodAttempts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getQuiz().getDifficulty().toString(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    long correct = list.stream()
                                            .filter(QuizAttempt::getIsCorrect).count();
                                    return list.isEmpty() ? 0.0 : 
                                            (double) correct / list.size() * 100;
                                }
                        )
                ));
        
        // 일별 학습량
        Map<LocalDateTime, Long> dailyAttempts = periodAttempts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAttemptedAt().toLocalDate().atStartOfDay(),
                        Collectors.counting()
                ));
        
        return Map.of(
                "period", period,
                "startDate", startDate,
                "totalAttempts", totalAttempts,
                "correctCount", correctCount,
                "accuracy", accuracy,
                "accuracyByDifficulty", accuracyByDifficulty,
                "dailyAttempts", dailyAttempts
        );
    }
    
    private int calculateScore(Quiz.Difficulty difficulty, boolean isCorrect) {
        if (!isCorrect) return 0;
        
        return switch (difficulty) {
            case EASY -> 10;
            case MEDIUM -> 20;
            case HARD -> 30;
        };
    }
    
    private LocalDateTime calculateStartDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period.toUpperCase()) {
            case "DAY" -> now.minusDays(1);
            case "WEEK" -> now.minusWeeks(1);
            case "MONTH" -> now.minusMonths(1);
            case "YEAR" -> now.minusYears(1);
            default -> now.minusWeeks(1);
        };
    }
    
    private QuizAttemptResponse convertToAttemptResponse(QuizAttempt attempt) {
        return QuizAttemptResponse.builder()
                .id(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .question(attempt.getQuiz().getQuestion())
                .userAnswer(attempt.getUserAnswer())
                .correctAnswer(attempt.getQuiz().getCorrectAnswer())
                .isCorrect(attempt.getIsCorrect())
                .score(attempt.getScore())
                .explanation(attempt.getQuiz().getExplanation())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }
    
    private WrongAnswerNoteResponse convertToWrongAnswerNote(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        StudyMaterial material = quiz.getStudyMaterial();
        
        return WrongAnswerNoteResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .materialId(material.getId())
                .materialTitle(material.getTitle())
                .question(quiz.getQuestion())
                .userAnswer(attempt.getUserAnswer())
                .correctAnswer(quiz.getCorrectAnswer())
                .explanation(quiz.getExplanation())
                .difficulty(quiz.getDifficulty().toString())
                .attemptedAt(attempt.getAttemptedAt())
                .attemptCount(quizAttemptRepository.countByUserIdAndQuizId(
                        attempt.getUser().getId(), quiz.getId()))
                .build();
    }
}
