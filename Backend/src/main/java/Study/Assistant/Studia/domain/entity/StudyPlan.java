package Study.Assistant.Studia.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "study_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String type; // class, exam, study
    
    @Column(nullable = false)
    private LocalDate date;
    
    private LocalTime startTime;
    
    private LocalTime endTime;
    
    private boolean allDay;
    
    private String color;
    
    private boolean isRepeat;
    
    private String repeatType; // weekly, biweekly, monthly
    
    private LocalDate repeatUntil;
    
    @Column(columnDefinition = "TEXT")
    private String repeatDays; // JSON array of days
    
    @Column(name = "class_name")
    private String className;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String groupId;
    
    @Column(name = "repeat_group_id")
    private String repeatGroupId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
