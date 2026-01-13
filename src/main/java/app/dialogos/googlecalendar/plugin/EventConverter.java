package app.dialogos.googlecalendar.plugin;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * EventConverter - Utility-Klasse zur Konvertierung zwischen EventRequest und Google Calendar Event.
 * 
 * Zentrale Stelle für alle Event-Konversions-Logik.
 * Wird von allen Nodes verwendet, die Events manipulieren (Create, Update, etc.).
 * 
 * Verantwortlichkeiten:
 * ├─ EventRequest → Google Calendar Event
 * ├─ LocalDateTime → Google DateTime Konvertierung
 * ├─ Reminders-Handling
 * └─ Konsistente DateTime-Formate
 */
public class EventConverter {

    /**
     * Konvertiert ein EventRequest zu einem Google Calendar Event.
     * 
     * @param request EventRequest mit allen Event-Daten
     * @return Google Calendar Event (bereit für insert/update)
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

        // Konvertiere Start-Zeit
        event.setStart(createEventDateTime(request.getStartTime()));

        // Konvertiere End-Zeit
        event.setEnd(createEventDateTime(request.getEndTime()));

        // Setze Reminders
        if (request.getReminders() != null && !request.getReminders().isEmpty()) {
            event.setReminders(new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(request.getReminders())
            );
        }

        return event;
    }

    /**
     * Konvertiert ein Google Calendar Event zurück zu EventRequest.
     * 
     * Nützlich wenn ein Event geladen wird und damit weiter arbeitet werden soll.
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
     * Konvertiert LocalDateTime zu Google EventDateTime.
     */
    public static EventDateTime createEventDateTime(LocalDateTime localDateTime) {
        EventDateTime eventDateTime = new EventDateTime();
        eventDateTime.setDateTime(
                new DateTime(Timestamp.valueOf(localDateTime))
        );
        return eventDateTime;
    }

    /**
     * Konvertiert Google EventDateTime zu LocalDateTime.
     * Handles sowohl DateTime (mit Zeit) als auch Date (ohne Zeit).
     */
    public static LocalDateTime googleDateTimeToLocalDateTime(EventDateTime eventDateTime) {
        if (eventDateTime == null) {
            return null;
        }

        if (eventDateTime.getDateTime() != null) {
            // DateTime mit Stunden/Minuten/Sekunden
            return new java.util.Date(eventDateTime.getDateTime().getValue())
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        } else if (eventDateTime.getDate() != null) {
            // Nur Datum (ohne Zeit)
            return new java.util.Date(eventDateTime.getDate().getValue())
                    .toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        }

        return null;
    }

    /**
     * Prüft ob zwei Events gleich sind (Vergleich der wichtigsten Felder).
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
     * Sicherer String-Vergleich (null-safe).
     */
    private static boolean safeEquals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return true;
        if (obj1 == null || obj2 == null) return false;
        return obj1.equals(obj2);
    }

    /**
     * Gibt eine lesbare Zusammenfassung eines Events aus.
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