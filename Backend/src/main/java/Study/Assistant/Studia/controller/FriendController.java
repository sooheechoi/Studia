package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.response.FriendResponse;
import Study.Assistant.Studia.dto.response.UserSearchResponse;
import Study.Assistant.Studia.security.JwtTokenProvider;
import Study.Assistant.Studia.service.FriendService;
import Study.Assistant.Studia.service.FriendRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class FriendController {
    
    private final FriendService friendService;
    private final FriendRecommendationService recommendationService;
    private final JwtTokenProvider jwtTokenProvider;
    
    // Search users
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String query,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        List<UserSearchResponse> users = friendService.searchUsers(userId, query);
        return ResponseEntity.ok(users);
    }
    
    // Get friend recommendations
    @GetMapping("/recommendations")
    public ResponseEntity<List<UserSearchResponse>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        List<UserSearchResponse> recommendations = recommendationService.getRecommendations(userId, limit);
        return ResponseEntity.ok(recommendations);
    }
    
    // Get all friends
    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        List<FriendResponse> friends = friendService.getFriends(userId);
        return ResponseEntity.ok(friends);
    }
    
    // Get pending friend requests
    @GetMapping("/requests")
    public ResponseEntity<Map<String, Object>> getPendingRequests(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        List<FriendResponse> requests = friendService.getPendingRequests(userId);
        Long count = friendService.countPendingRequests(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("requests", requests);
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }
    
    // Send friend request
    @PostMapping("/request/{targetUserId}")
    public ResponseEntity<Map<String, String>> sendFriendRequest(
            @PathVariable Long targetUserId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            friendService.sendFriendRequest(userId, targetUserId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Friend request sent successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Accept friend request
    @PostMapping("/accept/{requestId}")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            friendService.acceptFriendRequest(userId, requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Friend request accepted");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Decline friend request
    @PostMapping("/decline/{requestId}")
    public ResponseEntity<Map<String, String>> declineFriendRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            friendService.declineFriendRequest(userId, requestId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Friend request declined");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // Remove friend
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Map<String, String>> removeFriend(
            @PathVariable Long friendId,
            HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        
        try {
            friendService.removeFriend(userId, friendId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Friend removed successfully");
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
