package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.*;
import Study.Assistant.Studia.dto.request.StudyGroupRequest;
import Study.Assistant.Studia.dto.response.StudyGroupResponse;
import Study.Assistant.Studia.dto.response.GroupMessageResponse;
import Study.Assistant.Studia.dto.response.GroupMemberResponse;
import Study.Assistant.Studia.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StudyGroupService {
    
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;
    
    // Create a new study group
    public StudyGroupResponse createGroup(Long userId, StudyGroupRequest request) {
        User owner = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        }
        
        StudyGroup group = new StudyGroup(request.getName(), request.getDescription(), owner);
        group.setCourse(course);
        group.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 10);
        group.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : true);
        
        StudyGroup savedGroup = studyGroupRepository.save(group);
        
        log.info("Study group {} created by user {}", savedGroup.getId(), userId);
        return mapToResponse(savedGroup);
    }
    
    // Join a study group
    public void joinGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if already a member
        if (groupMemberRepository.isMember(group, user)) {
            throw new IllegalArgumentException("Already a member of this group");
        }
        
        // Check if group is full
        if (group.isFull()) {
            throw new IllegalArgumentException("Study group is full");
        }
        
        GroupMember member = new GroupMember(group, user, GroupMember.Role.MEMBER);
        member.setStatus(group.getIsPublic() ? GroupMember.Status.ACTIVE : GroupMember.Status.PENDING);
        
        groupMemberRepository.save(member);
        
        // Send notification to group owner
        if (!group.getIsPublic()) {
            notificationService.sendNotification(group.getOwner().getId(),
                "Group Join Request",
                user.getName() + " requested to join " + group.getName());
        }
        
        log.info("User {} joined group {}", userId, groupId);
    }
    
    // Leave a study group
    public void leaveGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        GroupMember member = groupMemberRepository.findByStudyGroupAndUser(group, user)
            .orElseThrow(() -> new IllegalArgumentException("Not a member of this group"));
        
        // Cannot leave if owner
        if (group.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Group owner cannot leave. Transfer ownership or delete the group.");
        }
        
        member.leave();
        groupMemberRepository.save(member);
        
        log.info("User {} left group {}", userId, groupId);
    }
    
    // Get user's study groups
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> getUserGroups(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<StudyGroup> groups = studyGroupRepository.findUserGroups(user);
        
        return groups.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Get public groups
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> getPublicGroups() {
        List<StudyGroup> groups = studyGroupRepository.findByIsPublicTrueAndIsActiveTrue();
        
        return groups.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Search groups
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> searchGroups(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getPublicGroups();
        }
        
        List<StudyGroup> groups = studyGroupRepository.searchGroups(query);
        
        return groups.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Send message to group (original method)
    public GroupMessageResponse sendMessage(Long userId, Long groupId, String content) {
        User sender = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is a member
        if (!groupMemberRepository.isMember(group, sender)) {
            throw new IllegalArgumentException("Not a member of this group");
        }
        
        GroupMessage message = new GroupMessage(group, sender, content);
        GroupMessage savedMessage = groupMessageRepository.save(message);
        
        // Send notifications to other members (in a real app, this would be via WebSocket)
        group.getMembers().stream()
            .filter(member -> member.isActive() && !member.getUser().getId().equals(userId))
            .forEach(member -> {
                notificationService.sendNotification(member.getUser().getId(),
                    "New Message in " + group.getName(),
                    sender.getName() + ": " + content.substring(0, Math.min(content.length(), 50)));
            });
        
        return mapMessageToResponse(savedMessage);
    }
    
    // Send message to group (WebSocket version)
    public GroupMessage sendMessageForWebSocket(Long groupId, Long senderId, String content) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is a member
        if (!groupMemberRepository.isMember(group, sender)) {
            throw new IllegalArgumentException("Not a member of this group");
        }
        
        GroupMessage message = new GroupMessage(group, sender, content);
        return groupMessageRepository.save(message);
    }
    
    // Get group messages
    @Transactional(readOnly = true)
    public Page<GroupMessageResponse> getGroupMessages(Long userId, Long groupId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is a member
        if (!groupMemberRepository.isMember(group, user)) {
            throw new IllegalArgumentException("Not a member of this group");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupMessage> messages = groupMessageRepository.findByStudyGroupAndIsDeletedFalseOrderBySentAtDesc(group, pageable);
        
        return messages.map(this::mapMessageToResponse);
    }
    
    // Get group details
    @Transactional(readOnly = true)
    public StudyGroupResponse getGroupDetails(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is a member (optional, depending on privacy settings)
        // For now, allow anyone to view public groups
        if (!group.getIsPublic() && !groupMemberRepository.isMember(group, user)) {
            throw new IllegalArgumentException("Not authorized to view this group");
        }
        
        return mapToResponse(group);
    }
    
    // Get group members
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembers(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is a member
        if (!groupMemberRepository.isMember(group, user)) {
            throw new IllegalArgumentException("Not a member of this group");
        }
        
        List<GroupMember> members = groupMemberRepository.findByStudyGroupAndStatus(group, GroupMember.Status.ACTIVE);
        
        return members.stream()
            .map(member -> GroupMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .name(member.getUser().getName())
                .email(member.getUser().getEmail())
                .profileImage(member.getUser().getProfileImage())
                .role(member.getRole().toString())
                .joinedAt(member.getJoinedAt())
                .isOnline(false) // TODO: Implement online status tracking
                .build())
            .collect(Collectors.toList());
    }
    
    // Invite users to group
    public List<Long> inviteToGroup(Long inviterId, Long groupId, List<Long> userIds) {
        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if inviter has permission (owner or admin)
        GroupMember inviterMember = groupMemberRepository.findByStudyGroupAndUser(group, inviter)
            .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));
        
        if (inviterMember.getRole() != GroupMember.Role.OWNER && 
            inviterMember.getRole() != GroupMember.Role.ADMIN) {
            throw new IllegalArgumentException("Only group owner or admin can invite members");
        }
        
        List<Long> invitedUserIds = new ArrayList<>();
        
        for (Long userId : userIds) {
            try {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                
                // Check if already a member
                if (groupMemberRepository.isMember(group, user)) {
                    continue;
                }
                
                // Create invitation
                GroupMember invitation = new GroupMember(group, user, GroupMember.Role.MEMBER);
                invitation.setStatus(GroupMember.Status.INVITED);
                groupMemberRepository.save(invitation);
                
                // Send notification
                notificationService.sendNotification(userId,
                    "Group Invitation",
                    inviter.getName() + " invited you to join " + group.getName());
                
                invitedUserIds.add(userId);
            } catch (Exception e) {
                log.error("Failed to invite user {}: {}", userId, e.getMessage());
            }
        }
        
        return invitedUserIds;
    }
    
    // Accept group invitation
    public void acceptInvitation(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        GroupMember invitation = groupMemberRepository.findByStudyGroupAndUser(group, user)
            .orElseThrow(() -> new IllegalArgumentException("No invitation found"));
        
        if (invitation.getStatus() != GroupMember.Status.INVITED) {
            throw new IllegalArgumentException("Invalid invitation status");
        }
        
        invitation.setStatus(GroupMember.Status.ACTIVE);
        groupMemberRepository.save(invitation);
        
        log.info("User {} accepted invitation to group {}", userId, groupId);
    }
    
    // Decline group invitation
    public void declineInvitation(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        GroupMember invitation = groupMemberRepository.findByStudyGroupAndUser(group, user)
            .orElseThrow(() -> new IllegalArgumentException("No invitation found"));
        
        if (invitation.getStatus() != GroupMember.Status.INVITED) {
            throw new IllegalArgumentException("Invalid invitation status");
        }
        
        groupMemberRepository.delete(invitation);
        
        log.info("User {} declined invitation to group {}", userId, groupId);
    }
    
    // Update group settings
    public StudyGroupResponse updateGroup(Long userId, Long groupId, StudyGroupRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is owner
        if (!group.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Only group owner can update settings");
        }
        
        // Update fields
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : group.getMaxMembers());
        group.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : group.getIsPublic());
        
        if (request.getCourseId() != null) {
            Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
            group.setCourse(course);
        }
        
        StudyGroup updatedGroup = studyGroupRepository.save(group);
        
        log.info("Group {} updated by user {}", groupId, userId);
        return mapToResponse(updatedGroup);
    }
    
    // Delete group
    public void deleteGroup(Long userId, Long groupId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if user is owner
        if (!group.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Only group owner can delete the group");
        }
        
        // Soft delete
        group.setIsActive(false);
        studyGroupRepository.save(group);
        
        // Notify all members
        group.getMembers().stream()
            .filter(member -> member.isActive())
            .forEach(member -> {
                notificationService.sendNotification(member.getUser().getId(),
                    "Group Deleted",
                    "The group '" + group.getName() + "' has been deleted by the owner");
            });
        
        log.info("Group {} deleted by owner {}", groupId, userId);
    }
    
    // Promote member to admin
    public void promoteMember(Long promoterId, Long groupId, Long memberId) {
        User promoter = userRepository.findById(promoterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if promoter is owner
        if (!group.getOwner().getId().equals(promoterId)) {
            throw new IllegalArgumentException("Only group owner can promote members");
        }
        
        User memberUser = userRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        
        GroupMember member = groupMemberRepository.findByStudyGroupAndUser(group, memberUser)
            .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group"));
        
        member.setRole(GroupMember.Role.ADMIN);
        groupMemberRepository.save(member);
        
        // Notify the promoted member
        notificationService.sendNotification(memberId,
            "Promoted to Admin",
            "You have been promoted to admin in " + group.getName());
        
        log.info("Member {} promoted to admin in group {} by {}", memberId, groupId, promoterId);
    }
    
    // Remove member from group
    public void removeMember(Long removerId, Long groupId, Long memberId) {
        User remover = userRepository.findById(removerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        StudyGroup group = studyGroupRepository.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Study group not found"));
        
        // Check if remover has permission (owner or admin)
        GroupMember removerMember = groupMemberRepository.findByStudyGroupAndUser(group, remover)
            .orElseThrow(() -> new IllegalArgumentException("You are not a member of this group"));
        
        if (removerMember.getRole() != GroupMember.Role.OWNER && 
            removerMember.getRole() != GroupMember.Role.ADMIN) {
            throw new IllegalArgumentException("Only group owner or admin can remove members");
        }
        
        User memberUser = userRepository.findById(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        
        GroupMember member = groupMemberRepository.findByStudyGroupAndUser(group, memberUser)
            .orElseThrow(() -> new IllegalArgumentException("User is not a member of this group"));
        
        // Cannot remove owner
        if (member.getRole() == GroupMember.Role.OWNER) {
            throw new IllegalArgumentException("Cannot remove group owner");
        }
        
        // Admin can only remove regular members
        if (removerMember.getRole() == GroupMember.Role.ADMIN && 
            member.getRole() == GroupMember.Role.ADMIN) {
            throw new IllegalArgumentException("Admin cannot remove another admin");
        }
        
        member.leave();
        groupMemberRepository.save(member);
        
        // Notify the removed member
        notificationService.sendNotification(memberId,
            "Removed from Group",
            "You have been removed from " + group.getName());
        
        log.info("Member {} removed from group {} by {}", memberId, groupId, removerId);
    }
    
    // Helper method to map StudyGroup to Response
    private StudyGroupResponse mapToResponse(StudyGroup group) {
        return StudyGroupResponse.builder()
            .id(group.getId())
            .name(group.getName())
            .description(group.getDescription())
            .courseId(group.getCourse() != null ? group.getCourse().getId() : null)
            .courseName(group.getCourse() != null ? group.getCourse().getCourseName() : null)
            .ownerId(group.getOwner().getId())
            .ownerName(group.getOwner().getName())
            .memberCount(group.getMemberCount())
            .maxMembers(group.getMaxMembers())
            .isPublic(group.getIsPublic())
            .isActive(group.getIsActive())
            .createdAt(group.getCreatedAt())
            .build();
    }
    
    // Helper method to map GroupMessage to Response
    private GroupMessageResponse mapMessageToResponse(GroupMessage message) {
        return GroupMessageResponse.builder()
            .id(message.getId())
            .senderId(message.getSender().getId())
            .senderName(message.getSender().getName())
            .senderImage(message.getSender().getProfileImage())
            .content(message.getContent())
            .type(message.getType().toString())
            .isEdited(message.getIsEdited())
            .sentAt(message.getSentAt())
            .editedAt(message.getEditedAt())
            .build();
    }
}
