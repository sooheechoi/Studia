package Study.Assistant.Studia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup studyGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @CreationTimestamp
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "invited_by")
    private Long invitedBy;

    public enum Role {
        OWNER,
        ADMIN,
        MODERATOR,
        MEMBER
    }

    public enum Status {
        PENDING,
        ACTIVE,
        INVITED,
        LEFT,
        KICKED
    }

    // Constructor
    public GroupMember(StudyGroup studyGroup, User user, Role role) {
        this.studyGroup = studyGroup;
        this.user = user;
        this.role = role;
        this.status = Status.ACTIVE; // Owners are automatically active
    }

    // Helper methods
    public void accept() {
        this.status = Status.ACTIVE;
    }

    public void leave() {
        this.status = Status.LEFT;
    }

    public void kick() {
        this.status = Status.KICKED;
    }

    public boolean isActive() {
        return this.status == Status.ACTIVE;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public boolean isModerator() {
        return this.role == Role.MODERATOR || this.role == Role.ADMIN;
    }
}
