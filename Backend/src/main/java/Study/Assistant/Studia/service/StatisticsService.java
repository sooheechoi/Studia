package Study.Assistant.Studia.service;

import Study.Assistant.Studia.dto.StudyStatisticsDto;
import Study.Assistant.Studia.domain.entity.*;
import Study.Assistant.Studia.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatisticsService {
    
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final SummaryRepository summaryRepository;
    private final UserRepository userRepository;
    
    public StudyStatisticsDto getUserStatistics(Long userId) {
        log.info("Generating statistics for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<QuizAttempt> allAttempts = quizAttemptRepository.findByUserIdOrderByAttemptedAtDesc(userId);
        
        // Get all quizzes from user's attempts
        List<Quiz> userQuizzes = allAttempts.stream()
                .map(QuizAttempt::getQuiz)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        List<Summary> userSummaries = summaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        // Calculate overall statistics
        int totalQuizzesTaken = allAttempts.size();
        int totalQuestionsAnswered = allAttempts.size(); // Since each attempt is one question
        
        double overallAccuracy = calculateOverallAccuracy(allAttempts);
        int totalStudyTime = calculateTotalStudyTime(allAttempts);
        int currentStreak = calculateCurrentStreak(allAttempts);
        int longestStreak = calculateLongestStreak(allAttempts);
        
        // Weekly statistics
        StudyStatisticsDto.WeeklyStatistics weeklyStats = calculateWeeklyStatistics(allAttempts);
        
        // Monthly statistics
        StudyStatisticsDto.MonthlyStatistics monthlyStats = calculateMonthlyStatistics(allAttempts, userQuizzes);
        
        // Subject statistics
        List<StudyStatisticsDto.SubjectStatistics> subjectStats = calculateSubjectStatistics(allAttempts, userQuizzes);
        
        // Recent activities
        List<StudyStatisticsDto.RecentActivity> recentActivities = getRecentActivities(allAttempts, userSummaries);
        
        return StudyStatisticsDto.builder()
                .totalQuizzesTaken(totalQuizzesTaken)
                .totalQuestionsAnswered(totalQuestionsAnswered)
                .overallAccuracy(overallAccuracy)
                .totalStudyTime(totalStudyTime)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .weeklyStats(weeklyStats)
                .monthlyStats(monthlyStats)
                .subjectStats(subjectStats)
                .recentActivities(recentActivities)
                .build();
    }
    
    private double calculateOverallAccuracy(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0.0;
        
        long correctCount = attempts.stream()
                .filter(attempt -> attempt.getIsCorrect() != null && attempt.getIsCorrect())
                .count();
        
        return (double) correctCount / attempts.size() * 100;
    }
    
    private int calculateTotalStudyTime(List<QuizAttempt> attempts) {
        // Estimate based on quiz attempts (10 minutes per quiz average)
        return attempts.size() * 10;
    }
    
    private int calculateCurrentStreak(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0;
        
        attempts.sort(Comparator.comparing(QuizAttempt::getAttemptedAt).reversed());
        
        LocalDate today = LocalDate.now();
        LocalDate lastAttemptDate = attempts.get(0).getAttemptedAt().toLocalDate();
        
        if (ChronoUnit.DAYS.between(lastAttemptDate, today) > 1) {
            return 0;
        }
        
        int streak = 1;
        LocalDate currentDate = lastAttemptDate;
        
        for (int i = 1; i < attempts.size(); i++) {
            LocalDate attemptDate = attempts.get(i).getAttemptedAt().toLocalDate();
            long dayDiff = ChronoUnit.DAYS.between(attemptDate, currentDate);
            
            if (dayDiff == 1) {
                streak++;
                currentDate = attemptDate;
            } else if (dayDiff > 1) {
                break;
            }
        }
        
        return streak;
    }
    
    private int calculateLongestStreak(List<QuizAttempt> attempts) {
        if (attempts.isEmpty()) return 0;
        
        attempts.sort(Comparator.comparing(QuizAttempt::getAttemptedAt));
        
        int maxStreak = 1;
        int currentStreak = 1;
        LocalDate lastDate = attempts.get(0).getAttemptedAt().toLocalDate();
        
        for (int i = 1; i < attempts.size(); i++) {
            LocalDate currentDate = attempts.get(i).getAttemptedAt().toLocalDate();
            long dayDiff = ChronoUnit.DAYS.between(lastDate, currentDate);
            
            if (dayDiff == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else if (dayDiff > 1) {
                currentStreak = 1;
            }
            
            lastDate = currentDate;
        }
        
        return maxStreak;
    }
    
    private StudyStatisticsDto.WeeklyStatistics calculateWeeklyStatistics(List<QuizAttempt> attempts) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        
        Map<String, Integer> dailyQuizCount = new LinkedHashMap<>();
        Map<String, Double> dailyAccuracy = new LinkedHashMap<>();
        Map<String, Integer> dailyStudyTime = new LinkedHashMap<>();
        
        // Initialize days of the week
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            String day = date.getDayOfWeek().toString();
            dailyQuizCount.put(day, 0);
            dailyAccuracy.put(day, 0.0);
            dailyStudyTime.put(day, 0);
        }
        
        // Filter attempts for current week
        List<QuizAttempt> weeklyAttempts = attempts.stream()
                .filter(a -> !a.getAttemptedAt().toLocalDate().isBefore(weekStart) &&
                           !a.getAttemptedAt().toLocalDate().isAfter(weekEnd))
                .collect(Collectors.toList());
        
        // Calculate daily statistics
        Map<DayOfWeek, List<QuizAttempt>> attemptsByDay = weeklyAttempts.stream()
                .collect(Collectors.groupingBy(a -> a.getAttemptedAt().getDayOfWeek()));
        
        for (Map.Entry<DayOfWeek, List<QuizAttempt>> entry : attemptsByDay.entrySet()) {
            String day = entry.getKey().toString();
            List<QuizAttempt> dayAttempts = entry.getValue();
            
            dailyQuizCount.put(day, dayAttempts.size());
            dailyAccuracy.put(day, calculateOverallAccuracy(dayAttempts));
            dailyStudyTime.put(day, dayAttempts.size() * 10); // 10 minutes per quiz
        }
        
        int totalQuizzes = weeklyAttempts.size();
        double averageAccuracy = calculateOverallAccuracy(weeklyAttempts);
        
        return StudyStatisticsDto.WeeklyStatistics.builder()
                .dailyQuizCount(dailyQuizCount)
                .dailyAccuracy(dailyAccuracy)
                .dailyStudyTime(dailyStudyTime)
                .totalQuizzes(totalQuizzes)
                .averageAccuracy(averageAccuracy)
                .build();
    }
    
    private StudyStatisticsDto.MonthlyStatistics calculateMonthlyStatistics(
            List<QuizAttempt> attempts, List<Quiz> quizzes) {
        
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        
        Map<String, Integer> weeklyQuizCount = new LinkedHashMap<>();
        Map<String, Double> weeklyAccuracy = new LinkedHashMap<>();
        Map<String, Integer> weeklyStudyTime = new LinkedHashMap<>();
        
        // Initialize weeks
        for (int week = 1; week <= 4; week++) {
            weeklyQuizCount.put("Week " + week, 0);
            weeklyAccuracy.put("Week " + week, 0.0);
            weeklyStudyTime.put("Week " + week, 0);
        }
        
        // Filter attempts for current month
        List<QuizAttempt> monthlyAttempts = attempts.stream()
                .filter(a -> !a.getAttemptedAt().toLocalDate().isBefore(monthStart) &&
                           !a.getAttemptedAt().toLocalDate().isAfter(monthEnd))
                .collect(Collectors.toList());
        
        // Group by week
        for (QuizAttempt attempt : monthlyAttempts) {
            int weekOfMonth = ((attempt.getAttemptedAt().getDayOfMonth() - 1) / 7) + 1;
            String weekKey = "Week " + Math.min(weekOfMonth, 4);
            
            weeklyQuizCount.merge(weekKey, 1, Integer::sum);
            weeklyStudyTime.merge(weekKey, 10, Integer::sum); // 10 minutes per quiz
        }
        
        // Calculate weekly accuracy
        for (int week = 1; week <= 4; week++) {
            String weekKey = "Week " + week;
            int weekNum = week;
            
            List<QuizAttempt> weekAttempts = monthlyAttempts.stream()
                    .filter(a -> ((a.getAttemptedAt().getDayOfMonth() - 1) / 7) + 1 == weekNum)
                    .collect(Collectors.toList());
            
            if (!weekAttempts.isEmpty()) {
                weeklyAccuracy.put(weekKey, calculateOverallAccuracy(weekAttempts));
            }
        }
        
        // Get top subjects
        Map<String, Long> subjectCounts = quizzes.stream()
                .filter(q -> q.getCategory() != null)
                .collect(Collectors.groupingBy(Quiz::getCategory, Collectors.counting()));
        
        List<String> topSubjects = subjectCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        int totalQuizzes = monthlyAttempts.size();
        double averageAccuracy = calculateOverallAccuracy(monthlyAttempts);
        
        return StudyStatisticsDto.MonthlyStatistics.builder()
                .weeklyQuizCount(weeklyQuizCount)
                .weeklyAccuracy(weeklyAccuracy)
                .weeklyStudyTime(weeklyStudyTime)
                .totalQuizzes(totalQuizzes)
                .averageAccuracy(averageAccuracy)
                .topSubjects(topSubjects)
                .build();
    }
    
    private List<StudyStatisticsDto.SubjectStatistics> calculateSubjectStatistics(
            List<QuizAttempt> attempts, List<Quiz> quizzes) {
        
        Map<String, List<QuizAttempt>> attemptsBySubject = new HashMap<>();
        Map<Long, Quiz> quizMap = quizzes.stream()
                .collect(Collectors.toMap(Quiz::getId, q -> q));
        
        // Group attempts by subject
        for (QuizAttempt attempt : attempts) {
            Quiz quiz = quizMap.get(attempt.getQuiz().getId());
            if (quiz != null && quiz.getCategory() != null) {
                attemptsBySubject.computeIfAbsent(quiz.getCategory(), k -> new ArrayList<>())
                        .add(attempt);
            }
        }
        
        List<StudyStatisticsDto.SubjectStatistics> subjectStats = new ArrayList<>();
        
        for (Map.Entry<String, List<QuizAttempt>> entry : attemptsBySubject.entrySet()) {
            String subject = entry.getKey();
            List<QuizAttempt> subjectAttempts = entry.getValue();
            
            int quizCount = subjectAttempts.size();
            int questionCount = subjectAttempts.size(); // Each attempt is one question
            double accuracy = calculateOverallAccuracy(subjectAttempts);
            int studyTime = quizCount * 10; // 10 minutes per quiz
            
            LocalDateTime lastStudied = subjectAttempts.stream()
                    .map(QuizAttempt::getAttemptedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            
            // Calculate improvement (compare first and last attempts)
            double improvement = calculateImprovement(subjectAttempts);
            
            subjectStats.add(StudyStatisticsDto.SubjectStatistics.builder()
                    .subject(subject)
                    .quizCount(quizCount)
                    .questionCount(questionCount)
                    .accuracy(accuracy)
                    .studyTime(studyTime)
                    .lastStudied(lastStudied)
                    .improvement(improvement)
                    .build());
        }
        
        return subjectStats;
    }
    
    private double calculateImprovement(List<QuizAttempt> attempts) {
        if (attempts.size() < 2) return 0.0;
        
        attempts.sort(Comparator.comparing(QuizAttempt::getAttemptedAt));
        
        // Calculate accuracy for first and last 25% of attempts
        int quarterSize = Math.max(1, attempts.size() / 4);
        List<QuizAttempt> firstQuarter = attempts.subList(0, quarterSize);
        List<QuizAttempt> lastQuarter = attempts.subList(attempts.size() - quarterSize, attempts.size());
        
        double firstAccuracy = calculateOverallAccuracy(firstQuarter);
        double lastAccuracy = calculateOverallAccuracy(lastQuarter);
        
        return lastAccuracy - firstAccuracy;
    }
    
    private List<StudyStatisticsDto.RecentActivity> getRecentActivities(
            List<QuizAttempt> attempts, List<Summary> summaries) {
        
        List<StudyStatisticsDto.RecentActivity> activities = new ArrayList<>();
        
        // Add quiz attempts
        for (QuizAttempt attempt : attempts.stream().limit(5).collect(Collectors.toList())) {
            Quiz quiz = attempt.getQuiz();
            Integer score = null;
            if (attempt.getScore() != null) {
                score = attempt.getScore();
            } else if (attempt.getIsCorrect() != null) {
                score = attempt.getIsCorrect() ? 100 : 0;
            }
            
            activities.add(StudyStatisticsDto.RecentActivity.builder()
                    .activityType("QUIZ_TAKEN")
                    .title(quiz != null ? "Quiz: " + quiz.getQuestion().substring(0, Math.min(50, quiz.getQuestion().length())) : "Quiz")
                    .subject(quiz != null ? quiz.getCategory() : null)
                    .timestamp(attempt.getAttemptedAt())
                    .score(score)
                    .build());
        }
        
        // Add summary creations
        for (Summary summary : summaries.stream().limit(5).collect(Collectors.toList())) {
            activities.add(StudyStatisticsDto.RecentActivity.builder()
                    .activityType("SUMMARY_CREATED")
                    .title(summary.getMaterialName())
                    .subject(summary.getCategory())
                    .timestamp(summary.getCreatedAt())
                    .score(null)
                    .build());
        }
        
        // Sort by timestamp descending
        activities.sort(Comparator.comparing(StudyStatisticsDto.RecentActivity::getTimestamp).reversed());
        
        return activities.stream().limit(10).collect(Collectors.toList());
    }
}
