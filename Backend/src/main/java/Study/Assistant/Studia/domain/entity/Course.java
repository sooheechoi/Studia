package Study.Assistant.Studia.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String courseName;
    
    private String courseCode;
    
    private String professor;
    
    @ElementCollection
    @CollectionTable(name = "course_schedules", joinColumns = @JoinColumn(name = "course_id"))
    private List<Schedule> schedules = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<StudyMaterial> studyMaterials = new ArrayList<>();
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<Exam> exams = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Integer credits;
    
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Schedule {
        @Enumerated(EnumType.STRING)
        private DayOfWeek dayOfWeek;
        
        private LocalTime startTime;
        
        private LocalTime endTime;
        
        private String location;
    }
}
