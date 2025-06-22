package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.*;
import Study.Assistant.Studia.dto.response.UserSearchResponse;
import Study.Assistant.Studia.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FriendRecommendationService {
    
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final CourseRepository courseRepository;
    private final StudyGroupRepository studyGroupRepository;
    
    /**
     * Get friend recommendations for a user based on:
     * 1. Mutual friends (friends of friends)
     * 2. Same courses
     * 3. Same study groups
     * 4. Similar interests (based on materials and quizzes)
     */
    public List<UserSearchResponse> getRecommendations(Long userId, int limit) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Map<Long, Integer> scoreMap = new HashMap<>();
        Set<Long> existingFriends = getExistingFriendIds(user);
        existingFriends.add(userId); // Exclude self
        
        // 1. Score based on mutual friends
        addMutualFriendsScore(user, scoreMap, existingFriends);
        
        // 2. Score based on same courses
        addSameCoursesScore(user, scoreMap, existingFriends);
        
        // 3. Score based on same study groups
        addSameGroupsScore(user, scoreMap, existingFriends);
        
        // 4. Score based on recent activity (users who joined recently)
        addRecentActivityScore(scoreMap, existingFriends);
        
        // Sort by score and get top recommendations
        List<Long> recommendedUserIds = scoreMap.entrySet().stream()
            .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Fetch user details
        List<User> recommendedUsers = userRepository.findAllById(recommendedUserIds);
        
        // Map to response maintaining the score order
        Map<Long, User> userMap = recommendedUsers.stream()
            .collect(Collectors.toMap(User::getId, u -> u));
        
        return recommendedUserIds.stream()
            .map(userMap::get)
            .filter(Objects::nonNull)
            .map(recommendedUser -> UserSearchResponse.builder()
                .id(recommendedUser.getId())
                .name(recommendedUser.getName())
                .email(recommendedUser.getEmail())
                .profileImage(recommendedUser.getProfileImage())
                .mutualFriendsCount(countMutualFriends(user, recommendedUser))
                .build())
            .collect(Collectors.toList());
    }
    
    private Set<Long> getExistingFriendIds(User user) {
        List<Friend> friends = friendRepository.findAcceptedFriends(user);
        Set<Long> friendIds = new HashSet<>();
        
        for (Friend friend : friends) {
            if (friend.getUser().getId().equals(user.getId())) {
                friendIds.add(friend.getFriend().getId());
            } else {
                friendIds.add(friend.getUser().getId());
            }
        }
        
        return friendIds;
    }
    
    private void addMutualFriendsScore(User user, Map<Long, Integer> scoreMap, Set<Long> excludeIds) {
        // Get friends of friends
        List<Friend> userFriends = friendRepository.findAcceptedFriends(user);
        
        for (Friend friend : userFriends) {
            User friendUser = friend.getUser().getId().equals(user.getId()) 
                ? friend.getFriend() : friend.getUser();
            
            List<Friend> friendsOfFriend = friendRepository.findAcceptedFriends(friendUser);
            
            for (Friend fof : friendsOfFriend) {
                User potentialFriend = fof.getUser().getId().equals(friendUser.getId()) 
                    ? fof.getFriend() : fof.getUser();
                
                if (!excludeIds.contains(potentialFriend.getId())) {
                    scoreMap.merge(potentialFriend.getId(), 10, Integer::sum); // 10 points per mutual friend
                }
            }
        }
    }
    
    private void addSameCoursesScore(User user, Map<Long, Integer> scoreMap, Set<Long> excludeIds) {
        // Get user's courses through study materials and quizzes
        List<Course> userCourses = courseRepository.findCoursesForUser(user.getId());
        
        for (Course course : userCourses) {
            List<User> courseUsers = userRepository.findUsersByCourse(course.getId());
            
            for (User courseUser : courseUsers) {
                if (!excludeIds.contains(courseUser.getId())) {
                    scoreMap.merge(courseUser.getId(), 5, Integer::sum); // 5 points per shared course
                }
            }
        }
    }
    
    private void addSameGroupsScore(User user, Map<Long, Integer> scoreMap, Set<Long> excludeIds) {
        // Get user's study groups
        List<StudyGroup> userGroups = studyGroupRepository.findUserGroups(user);
        
        for (StudyGroup group : userGroups) {
            List<GroupMember> members = groupMemberRepository.findByStudyGroupAndStatus(
                group, GroupMember.Status.ACTIVE);
            
            for (GroupMember member : members) {
                if (!excludeIds.contains(member.getUser().getId())) {
                    scoreMap.merge(member.getUser().getId(), 7, Integer::sum); // 7 points per shared group
                }
            }
        }
    }
    
    private void addRecentActivityScore(Map<Long, Integer> scoreMap, Set<Long> excludeIds) {
        // Get recently active users (joined in the last 30 days)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = cal.getTime();
        
        List<User> recentUsers = userRepository.findByCreatedAtAfter(thirtyDaysAgo);
        
        for (User recentUser : recentUsers) {
            if (!excludeIds.contains(recentUser.getId())) {
                scoreMap.merge(recentUser.getId(), 3, Integer::sum); // 3 points for being recently active
            }
        }
    }
    
    private int countMutualFriends(User user1, User user2) {
        Set<Long> user1Friends = getExistingFriendIds(user1);
        Set<Long> user2Friends = getExistingFriendIds(user2);
        
        // Find intersection
        user1Friends.retainAll(user2Friends);
        return user1Friends.size();
    }
}
