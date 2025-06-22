package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.QuizAttempt;
import Study.Assistant.Studia.domain.entity.StudyMaterial;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.dto.response.StatsOverviewResponse;
import Study.Assistant.Studia.repository.QuizAttemptRepository;
import Study.Assistant.Studia.repository.StudyMaterialRepository;
import Study.Assistant.Studia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsService {
    
    private final UserRepository userRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    
    public StatsOverviewResponse getUserStats() {
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Calculate statistics
        List<StudyMaterial> materials = studyMaterialRepository.findByUserId(user.getId());
        List<QuizAttempt> attempts = quizAttemptRepository.findByUserId(user.getId());
        
        int totalMaterials = materials.size();
        int completedQuizzes = attempts.size();
        
        // Calculate average score
        double averageScore = 0.0;
        if (!attempts.isEmpty()) {
            double totalScore = attempts.stream()
                    .mapToDouble(QuizAttempt::getScore)
                    .sum();
            averageScore = totalScore / attempts.size();
        }
        
        // Calculate study streak (simplified - counts consecutive days with activity)
        int studyStreak = calculateStudyStreak(user.getId());
        
        // Count materials by status
        long processedMaterials = materials.stream()
                .filter(m -> m.getStatus() == StudyMaterial.ProcessingStatus.COMPLETED)
                .count();
        
        // Recent activity
        LocalDateTime lastActivity = calculateLastActivity(materials, attempts);
        
        return StatsOverviewResponse.builder()
                .totalMaterials(totalMaterials)
                .processedMaterials((int) processedMaterials)
                .completedQuizzes(completedQuizzes)
                .averageScore((int) Math.round(averageScore))
                .studyStreak(studyStreak)
                .lastActivity(lastActivity)
                .build();
    }
    
    private int calculateStudyStreak(Long userId) {
        // This is a simplified implementation
        // In a real application, you'd track daily activity more precisely
        List<QuizAttempt> recentAttempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
        
        if (recentAttempts.isEmpty()) {
            return 0;
        }
        
        int streak = 1;
        LocalDate previousDate = recentAttempts.get(0).getAttemptedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        
        // If last activity wasn't today or yesterday, streak is broken
        if (previousDate.isBefore(today.minusDays(1))) {
            return 0;
        }
        
        for (int i = 1; i < recentAttempts.size(); i++) {
            LocalDate currentDate = recentAttempts.get(i).getAttemptedAt().toLocalDate();
            
            if (previousDate.minusDays(1).equals(currentDate)) {
                streak++;
                previousDate = currentDate;
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    private LocalDateTime calculateLastActivity(List<StudyMaterial> materials, List<QuizAttempt> attempts) {
        LocalDateTime lastMaterialUpload = materials.stream()
                .map(StudyMaterial::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
        
        LocalDateTime lastQuizAttempt = attempts.stream()
                .map(QuizAttempt::getAttemptedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.MIN);
        
        return lastMaterialUpload.isAfter(lastQuizAttempt) ? lastMaterialUpload : lastQuizAttempt;
    }
}
