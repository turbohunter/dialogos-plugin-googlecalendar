package app.dialogos.googlecalendar.plugin;


import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;


import java.sql.Timestamp;
import java.time.LocalDateTime;


/**
 * EventConverter - Utility class for conversion between EventRequest and Google Calendar Event.
 * 
 * Central location for all event conversion logic.
 * Used by all nodes that manipulate events (Create, Update, etc.).
 * 
 * Responsibilities:
 * ├─ EventRequest → Google Calendar Event
 * ├─ LocalDateTime → Google DateTime conversion
 * ├─ Reminders handling
 * └─ Consistent DateTime formats
 */
public class EventConverter {


    /**
     * Converts an EventRequest to a Google Calendar Event.
     * 
     * @param request EventRequest with all event data
     * @return Google Calendar Event (ready for insert/update)
     */
    public static Event toGoogleCalendarEvent(EventRequest request) {
        Event event = new Event();
        event.setSummary(request.getSummary());


        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            event.setDescription(request.getDescription());
        }


        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            event.setLocation(request.getLocation());
        }


        // Convert start time
        event.setStart(createEventDateTime(request.getStartTime()));


        // Convert end time
        event.setEnd(createEventDateTime(request.getEndTime()));


        // Set reminders
        if (request.getReminders() != null && !request.getReminders().isEmpty()) {
            event.setReminders(new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(request.getReminders())
            );
        }


        return event;
    }


    /**
     * Converts a Google Calendar Event back to EventRequest.
     * 
     * Useful when an event is loaded and needs to be worked with further.
     * 
     * @param event Google Calendar Event
     * @return EventRequest
     */
    public static EventRequest toEventRequest(Event event) {
        EventRequest.Builder builder = EventRequest.builder()
                .summary(event.getSummary())
                .startTime(googleDateTimeToLocalDateTime(event.getStart()))
                .endTime(googleDateTimeToLocalDateTime(event.getEnd()));


        if (event.getDescription() != null) {
            builder.description(event.getDescription());
        }


        if (event.getLocation() != null) {
            builder.location(event.getLocation());
        }


        if (event.getReminders() != null && event.getReminders().getOverrides() != null) {
            builder.reminders(event.getReminders().getOverrides());
        }


        return builder.build();
    }


    /**
     * Converts LocalDateTime to Google EventDateTime.
     */
    public static EventDateTime createEventDateTime(LocalDateTime localDateTime) {
        EventDateTime eventDateTime = new EventDateTime();
        eventDateTime.setDateTime(
                new DateTime(Timestamp.valueOf(localDateTime))
        );
        return eventDateTime;
    }


    /**
     * Converts Google EventDateTime to LocalDateTime.
     * Handles both DateTime (with time) and Date (without time).
     */
    public static LocalDateTime googleDateTimeToLocalDateTime(EventDateTime eventDateTime) {
        if (eventDateTime == null) {
            return null;
        }


        if (eventDateTime.getDateTime() != null) {
            // DateTime with hours/minutes/seconds
            return new java.util.Date(eventDateTime.getDateTime().getValue())
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        } else if (eventDateTime.getDate() != null) {
            // Date only (without time)
            return new java.util.Date(eventDateTime.getDate().getValue())
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        }


        return null;
    }


    /**
     * Checks if two events are equal (compares most important fields).
     */
    public static boolean eventsEqual(Event event1, Event event2) {
        if (event1 == null || event2 == null) {
            return event1 == event2;
        }


        return safeEquals(event1.getSummary(), event2.getSummary())
                && safeEquals(event1.getDescription(), event2.getDescription())
                && safeEquals(event1.getLocation(), event2.getLocation());
    }


    /**
     * Safe string comparison (null-safe).
     */
    private static boolean safeEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null || obj2 == null) return false;
        return obj1.equals(obj2);
    }


    /**
     * Returns a readable summary of an event.
     */
    public static String eventToString(Event event) {
        StringBuilder sb = new StringBuilder();
        sb.append("Event{");
        if (event.getSummary() != null) {
            sb.append("title='").append(event.getSummary()).append("'");
        }
        if (event.getStart() != null) {
            sb.append(", start=").append(event.getStart());
        }
        if (event.getEnd() != null) {
            sb.append(", end=").append(event.getEnd());
        }
        if (event.getLocation() != null) {
            sb.append(", location='").append(event.getLocation()).append("'");
        }
        sb.append("}");
        return sb.toString();
    }
}
