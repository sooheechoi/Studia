package Study.Assistant.Studia.dto.request;

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
public class StudyPlanRequest {
    private String title;
    private String type; // class, exam, study
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean allDay;
    private String color;
    private boolean repeat;
    private String repeatType; // weekly, biweekly, monthly
    private LocalDate repeatUntil;
    private List<Integer> repeatDays; // days of week (0-6)
    private String className;
    private String description;
    private String groupId;
    private String repeatGroupId;
}
