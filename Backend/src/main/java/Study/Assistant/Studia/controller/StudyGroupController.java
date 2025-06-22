package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.request.StudyGroupRequest;
import Study.Assistant.Studia.dto.request.GroupInviteRequest;
import Study.Assistant.Studia.dto.response.StudyGroupResponse;
import Study.Assistant.Studia.dto.response.GroupMessageResponse;
import Study.Assistant.Studia.dto.response.GroupMemberResponse;
import Study.Assistant.Studia.security.JwtTokenProvider;
import Study.Assistant.Studia.service.StudyGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StudyGroupController {
    
    private final StudyGroupService studyGroupService;
    private final JwtTokenProvider jwtTokenProvider;
    
    // Create a new study group
    @PostMapping
    public ResponseEntity<StudyGroupResponse> createGroup(
            @Valid @RequestBody StudyGroupRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        StudyGroupResponse response = studyGroupService.createGroup(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // Get user's study groups
    @GetMapping("/my")
    public ResponseEntity<List<StudyGroupResponse>> getMyGroups(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        List<StudyGroupResponse> groups = studyGroupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }
    
    // Get public groups
    @GetMapping("/public")
    public ResponseEntity<List<StudyGroupResponse>> getPublicGroups() {
        List<StudyGroupResponse> groups = studyGroupService.getPublicGroups();
        return ResponseEntity.ok(groups);
    }
    
    // Search groups
    @GetMapping("/search")
    public ResponseEntity<List<StudyGroupResponse>> searchGroups(
            @RequestParam(required = false) String query) {
        List<StudyGroupResponse> groups = studyGroupService.searchGroups(query);
        return ResponseEntity.ok(groups);
    }
    
    // Get group details
    @GetMapping("/{groupId}")
    public ResponseEntity<StudyGroupResponse> getGroupDetails(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        StudyGroupResponse group = studyGroupService.getGroupDetails(userId, groupId);
        return ResponseEntity.ok(group);
    }
    
    // Join a group
    @PostMapping("/{groupId}/join")
    public ResponseEntity<Map<String, String>> joinGroup(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.joinGroup(userId, groupId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully joined the group");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Leave a group
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Map<String, String>> leaveGroup(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.leaveGroup(userId, groupId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully left the group");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Get group members
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        List<GroupMemberResponse> members = studyGroupService.getGroupMembers(userId, groupId);
        return ResponseEntity.ok(members);
    }
    
    // Get group messages
    @GetMapping("/{groupId}/messages")
    public ResponseEntity<Page<GroupMessageResponse>> getGroupMessages(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        Page<GroupMessageResponse> messages = studyGroupService.getGroupMessages(userId, groupId, page, size);
        return ResponseEntity.ok(messages);
    }
    
    // Send message to group (REST endpoint - can be used as fallback)
    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupMessageResponse> sendMessage(
            @PathVariable Long groupId,
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        String content = payload.get("content");
        
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        GroupMessageResponse response = studyGroupService.sendMessage(userId, groupId, content);
        return ResponseEntity.ok(response);
    }
    
    // Invite users to group
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<Map<String, Object>> inviteToGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupInviteRequest inviteRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            List<Long> invitedUserIds = studyGroupService.inviteToGroup(userId, groupId, inviteRequest.getUserIds());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invitations sent successfully");
            response.put("invitedCount", invitedUserIds.size());
            response.put("invitedUserIds", invitedUserIds);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Accept group invitation
    @PostMapping("/{groupId}/accept-invite")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.acceptInvitation(userId, groupId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invitation accepted");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Decline group invitation
    @PostMapping("/{groupId}/decline-invite")
    public ResponseEntity<Map<String, String>> declineInvitation(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.declineInvitation(userId, groupId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Invitation declined");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Update group settings (for group owner/admin)
    @PutMapping("/{groupId}")
    public ResponseEntity<StudyGroupResponse> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody StudyGroupRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromToken(httpRequest);
        
        try {
            StudyGroupResponse response = studyGroupService.updateGroup(userId, groupId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Delete group (for group owner only)
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Map<String, String>> deleteGroup(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.deleteGroup(userId, groupId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Group deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Promote member to admin
    @PostMapping("/{groupId}/members/{memberId}/promote")
    public ResponseEntity<Map<String, String>> promoteMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.promoteMember(userId, groupId, memberId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Member promoted to admin");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Remove member from group
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long memberId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            studyGroupService.removeMember(userId, groupId, memberId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Member removed from group");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Helper method to extract user ID from JWT token
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            return jwtTokenProvider.getUserId(token);
        }
        throw new IllegalArgumentException("Invalid token");
    }
}
