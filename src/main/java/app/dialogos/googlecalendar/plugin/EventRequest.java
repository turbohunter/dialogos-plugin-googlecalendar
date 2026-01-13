package app.dialogos.googlecalendar.plugin;

import com.google.api.services.calendar.model.EventReminder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventRequest {
    
    private String summary;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<EventReminder> reminders;
    
    // Private Constructor für Builder
    private EventRequest() {}
    
    // Getters
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<EventReminder> getReminders() { return reminders; }
    
    // Builder Pattern
    public static class Builder {
        private final EventRequest request = new EventRequest();
        
        public Builder summary(String summary) {
            request.summary = summary;
            return this;
        }
        
        public Builder description(String description) {
            request.description = description;
            return this;
        }
        
        public Builder location(String location) {
            request.location = location;
            return this;
        }
        
        public Builder startTime(LocalDateTime startTime) {
            request.startTime = startTime;
            return this;
        }
        
        public Builder endTime(LocalDateTime endTime) {
            request.endTime = endTime;
            return this;
        }
        
        public Builder reminders(List<EventReminder> reminders) {
            request.reminders = reminders;
            return this;
        }
        
        public Builder addReminder(String method, int minutes) {
            if (request.reminders == null) {
                request.reminders = new ArrayList<>();
            }
            request.reminders.add(
                new EventReminder().setMethod(method).setMinutes(minutes)
            );
            return this;
        }
        
        public EventRequest build() {
            if (request.summary == null || request.startTime == null || request.endTime == null) {
                throw new IllegalStateException("Summary, startTime und endTime sind Pflichtfelder");
            }
            return request;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
