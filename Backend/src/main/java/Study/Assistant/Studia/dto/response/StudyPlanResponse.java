package Study.Assistant.Studia.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanResponse {
    private Long id;
    private String title;
    private String type;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;
    
    private boolean allDay;
    private String color;
    private boolean repeat;
    private String repeatType;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate repeatUntil;
    
    private List<Integer> repeatDays;
    private String className;
    private String description;
    private String groupId;
    private String repeatGroupId;
    private String createdAt;
    private String updatedAt;
}
