package Study.Assistant.Studia.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String university;
    
    private String major;
    
    private Integer grade;
    
    @Column(name = "profile_image")
    private String profileImage;
    
    @Column(name = "status_message")
    private String statusMessage;
    
    @Column(name = "is_online")
    private Boolean isOnline = false;
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.STUDENT;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Course> courses = new ArrayList<>();
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<StudyMaterial> studyMaterials = new ArrayList<>();
    
    // Friend relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Friend> friendRequestsSent = new HashSet<>();
    
    @OneToMany(mappedBy = "friend", cascade = CascadeType.ALL)
    private Set<Friend> friendRequestsReceived = new HashSet<>();
    
    // Study group memberships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private Set<GroupMember> groupMemberships = new HashSet<>();
    
    // Owned study groups
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private Set<StudyGroup> ownedGroups = new HashSet<>();
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum UserRole {
        STUDENT, ADMIN
    }
}
