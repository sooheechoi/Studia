package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.StudyPlan;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.dto.request.StudyPlanRequest;
import Study.Assistant.Studia.dto.response.StudyPlanResponse;
import Study.Assistant.Studia.repository.StudyPlanRepository;
import Study.Assistant.Studia.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudyPlanService {
    
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    // Create a new study plan
    public StudyPlanResponse createStudyPlan(StudyPlanRequest request) {
        return createPlan(request);
    }
    
    // Get all study plans for the current user
    public List<StudyPlanResponse> getAllStudyPlans() {
        return getAllPlans();
    }
    
    // Get study plans by date range
    public List<StudyPlanResponse> getStudyPlansByDateRange(LocalDate startDate, LocalDate endDate) {
        return getPlansByDateRange(startDate, endDate);
    }
    
    // Get a specific study plan
    public StudyPlanResponse getStudyPlan(Long id) {
        User user = getCurrentUser();
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));
        
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to access this plan");
        }
        
        return convertToResponse(plan);
    }
    
    // Update a study plan
    public StudyPlanResponse updateStudyPlan(Long id, StudyPlanRequest request) {
        return updatePlan(id, request);
    }
    
    // Delete a study plan
    public void deleteStudyPlan(Long id) {
        deletePlan(id);
    }
    
    // Delete recurring study plans
    public void deleteRecurringStudyPlans(Long id, String groupId) {
        User user = getCurrentUser();
        
        if (groupId != null && !groupId.isEmpty()) {
            // Delete all plans with the same groupId
            List<StudyPlan> plans = studyPlanRepository.findByUserAndRepeatGroupId(user, groupId);
            studyPlanRepository.deleteAll(plans);
        } else {
            // Delete single plan
            deleteStudyPlan(id);
        }
    }
    
    // Update recurring study plans
    public List<StudyPlanResponse> updateRecurringStudyPlans(Long id, String groupId, StudyPlanRequest request) {
        User user = getCurrentUser();
        List<StudyPlan> updatedPlans = new ArrayList<>();
        
        if (groupId != null && !groupId.isEmpty()) {
            // Update all plans with the same groupId
            List<StudyPlan> plans = studyPlanRepository.findByUserAndRepeatGroupId(user, groupId);
            for (StudyPlan plan : plans) {
                updatePlanFields(plan, request);
                updatedPlans.add(studyPlanRepository.save(plan));
            }
        } else {
            // Update single plan
            StudyPlan plan = studyPlanRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Study plan not found"));
            
            if (!plan.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized to update this plan");
            }
            
            updatePlanFields(plan, request);
            updatedPlans.add(studyPlanRepository.save(plan));
        }
        
        return updatedPlans.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    // Legacy methods (keeping for compatibility)
    public List<StudyPlanResponse> getAllPlans() {
        User user = getCurrentUser();
        List<StudyPlan> plans = studyPlanRepository.findByUserOrderByDateAsc(user);
        return plans.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public List<StudyPlanResponse> getPlansByDateRange(LocalDate startDate, LocalDate endDate) {
        User user = getCurrentUser();
        List<StudyPlan> plans = studyPlanRepository.findByUserAndDateBetween(user, startDate, endDate);
        return plans.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    public StudyPlanResponse createPlan(StudyPlanRequest request) {
        User user = getCurrentUser();
        
        if (request.isRepeat()) {
            return createRecurringPlans(user, request);
        } else {
            StudyPlan plan = createSinglePlan(user, request);
            return convertToResponse(studyPlanRepository.save(plan));
        }
    }
    
    private StudyPlan createSinglePlan(User user, StudyPlanRequest request) {
        StudyPlan plan = StudyPlan.builder()
                .user(user)
                .title(request.getTitle())
                .type(request.getType())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .allDay(request.isAllDay())
                .color(request.getColor())
                .isRepeat(false)
                .className(request.getClassName())
                .description(request.getDescription())
                .groupId(request.getGroupId())
                .repeatGroupId(request.getRepeatGroupId())
                .build();
        
        return plan;
    }
    
    private StudyPlanResponse createRecurringPlans(User user, StudyPlanRequest request) {
        List<StudyPlan> plans = new ArrayList<>();
        LocalDate startDate = request.getDate();
        LocalDate endDate = request.getRepeatUntil() != null ? 
                request.getRepeatUntil() : startDate.plusMonths(3);
        
        // Generate a unique repeatGroupId for this set of recurring plans
        String repeatGroupId = "group_" + System.currentTimeMillis();
        
        LocalDate currentDate = startDate;
        int count = 0;
        
        while (!currentDate.isAfter(endDate) && count < 50) { // Limit to 50 occurrences
            if ("weekly".equals(request.getRepeatType()) && request.getRepeatDays() != null) {
                for (Integer dayOfWeek : request.getRepeatDays()) {
                    LocalDate planDate = currentDate.with(java.time.DayOfWeek.of(dayOfWeek == 0 ? 7 : dayOfWeek));
                    if (!planDate.isBefore(startDate) && !planDate.isAfter(endDate)) {
                        StudyPlan plan = createSinglePlanForDate(user, request, planDate);
                        plan.setRepeatGroupId(repeatGroupId);
                        plans.add(plan);
                        count++;
                    }
                }
                currentDate = currentDate.plusWeeks(1);
            } else if ("biweekly".equals(request.getRepeatType())) {
                StudyPlan plan = createSinglePlanForDate(user, request, currentDate);
                plan.setRepeatGroupId(repeatGroupId);
                plans.add(plan);
                currentDate = currentDate.plusWeeks(2);
                count++;
            } else if ("monthly".equals(request.getRepeatType())) {
                StudyPlan plan = createSinglePlanForDate(user, request, currentDate);
                plan.setRepeatGroupId(repeatGroupId);
                plans.add(plan);
                currentDate = currentDate.plusMonths(1);
                count++;
            }
        }
        
        studyPlanRepository.saveAll(plans);
        
        // Return the first created plan
        return plans.isEmpty() ? null : convertToResponse(plans.get(0));
    }
    
    private StudyPlan createSinglePlanForDate(User user, StudyPlanRequest request, LocalDate date) {
        StudyPlan plan = createSinglePlan(user, request);
        plan.setDate(date);
        plan.setRepeat(true);
        plan.setRepeatType(request.getRepeatType());
        plan.setRepeatUntil(request.getRepeatUntil());
        if (request.getRepeatDays() != null) {
            try {
                plan.setRepeatDays(objectMapper.writeValueAsString(request.getRepeatDays()));
            } catch (JsonProcessingException e) {
                log.error("Error serializing repeat days", e);
            }
        }
        return plan;
    }
    
    public StudyPlanResponse updatePlan(Long id, StudyPlanRequest request) {
        User user = getCurrentUser();
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));
        
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to update this plan");
        }
        
        updatePlanFields(plan, request);
        
        return convertToResponse(studyPlanRepository.save(plan));
    }
    
    private void updatePlanFields(StudyPlan plan, StudyPlanRequest request) {
        plan.setTitle(request.getTitle());
        plan.setType(request.getType());
        plan.setDate(request.getDate());
        plan.setStartTime(request.getStartTime());
        plan.setEndTime(request.getEndTime());
        plan.setAllDay(request.isAllDay());
        plan.setColor(request.getColor());
        plan.setClassName(request.getClassName());
        plan.setDescription(request.getDescription());
    }
    
    public void deletePlan(Long id) {
        User user = getCurrentUser();
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));
        
        if (!plan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this plan");
        }
        
        studyPlanRepository.delete(plan);
    }
    
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private StudyPlanResponse convertToResponse(StudyPlan plan) {
        List<Integer> repeatDays = null;
        if (plan.getRepeatDays() != null) {
            try {
                repeatDays = objectMapper.readValue(plan.getRepeatDays(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing repeat days", e);
            }
        }
        
        return StudyPlanResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .type(plan.getType())
                .date(plan.getDate())
                .startTime(plan.getStartTime())
                .endTime(plan.getEndTime())
                .allDay(plan.isAllDay())
                .color(plan.getColor())
                .repeat(plan.isRepeat())
                .repeatType(plan.getRepeatType())
                .repeatUntil(plan.getRepeatUntil())
                .repeatDays(repeatDays)
                .className(plan.getClassName())
                .description(plan.getDescription())
                .groupId(plan.getGroupId())
                .repeatGroupId(plan.getRepeatGroupId())
                .createdAt(plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null)
                .updatedAt(plan.getUpdatedAt() != null ? plan.getUpdatedAt().toString() : null)
                .build();
    }
}