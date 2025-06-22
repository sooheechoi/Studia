package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.dto.response.LeaderboardResponse;
import Study.Assistant.Studia.repository.QuizAttemptRepository;
import Study.Assistant.Studia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LeaderboardService {
    
    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    
    /**
     * 전체 리더보드 조회
     */
    public List<LeaderboardResponse> getGlobalLeaderboard(int limit) {
        // 모든 사용자의 퀴즈 점수 집계
        List<Object[]> userScores = quizAttemptRepository.findTopUsersByScore(limit);
        
        return IntStream.range(0, userScores.size())
                .mapToObj(i -> {
                    Object[] row = userScores.get(i);
                    Long userId = (Long) row[0];
                    User user = userRepository.findById(userId).orElse(null);
                    
                    if (user == null) return null;
                    
                    return LeaderboardResponse.builder()
                            .userId(userId)
                            .username(user.getName())
                            .totalScore(((Number) row[1]).intValue())
                            .totalQuizzes(((Number) row[2]).intValue())
                            .averageScore(((Number) row[3]).doubleValue())
                            .rank(i + 1)
                            .lastActivityAt(quizAttemptRepository.findLastActivityByUserId(userId))
                            .build();
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 코스별 리더보드 조회
     */
    public List<LeaderboardResponse.CourseLeaderboard> getCourseLeaderboard(Long courseId, int limit) {
        List<Object[]> courseScores = quizAttemptRepository.findTopUsersByCourse(courseId, limit);
        
        return IntStream.range(0, courseScores.size())
                .mapToObj(i -> {
                    Object[] row = courseScores.get(i);
                    Long userId = (Long) row[0];
                    User user = userRepository.findById(userId).orElse(null);
                    
                    if (user == null) return null;
                    
                    return LeaderboardResponse.CourseLeaderboard.builder()
                            .courseId(courseId)
                            .userId(userId)
                            .username(user.getName())
                            .totalScore(((Number) row[1]).intValue())
                            .quizzesTaken(((Number) row[2]).intValue())
                            .averageScore(((Number) row[3]).doubleValue())
                            .rank(i + 1)
                            .build();
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 현재 사용자의 순위 조회
     */
    public LeaderboardResponse getUserRank() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 사용자의 총 점수 계산
        Object[] userStats = quizAttemptRepository.findUserStats(user.getId());
        if (userStats == null || userStats[0] == null) {
            return LeaderboardResponse.builder()
                    .userId(user.getId())
                    .username(user.getName())
                    .totalScore(0)
                    .totalQuizzes(0)
                    .averageScore(0.0)
                    .rank(0)
                    .build();
        }
        
        int totalScore = ((Number) userStats[0]).intValue();
        int totalQuizzes = ((Number) userStats[1]).intValue();
        double averageScore = ((Number) userStats[2]).doubleValue();
        
        // 순위 계산
        int rank = quizAttemptRepository.findUserRank(totalScore) + 1;
        
        return LeaderboardResponse.builder()
                .userId(user.getId())
                .username(user.getName())
                .totalScore(totalScore)
                .totalQuizzes(totalQuizzes)
                .averageScore(averageScore)
                .rank(rank)
                .lastActivityAt(quizAttemptRepository.findLastActivityByUserId(user.getId()))
                .build();
    }
}
