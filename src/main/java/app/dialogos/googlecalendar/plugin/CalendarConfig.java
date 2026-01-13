package app.dialogos.googlecalendar.plugin;


/**
 * CalendarConfig - Immutable configuration class for Google Calendar.
 * 
 * This class contains all global settings that are shared by all nodes.
 * It is immutable for thread-safety.
 * 
 * Usage:
 * - Used by GoogleCalendarPluginRuntime to initialize the API
 * - Used by all Calendar Nodes to access configuration
 */
public final class CalendarConfig {
    
    private final String serviceAccountFile;
    private final String calendarId;
    private final String applicationName;
    
    /**
     * Creates a new calendar configuration.
     * 
     * @param serviceAccountFile Absolute path to the Google Service Account JSON file
     * @param calendarId Google Calendar ID
     * @param applicationName Name of the application for Google API requests
     */
    public CalendarConfig(String serviceAccountFile, String calendarId, String applicationName) {
        this.serviceAccountFile = serviceAccountFile;
        this.calendarId = calendarId;
        this.applicationName = applicationName;
    }
    
    /**
     * Returns the path to the service account file.
     */
    public String getServiceAccountFile() {
        return serviceAccountFile;
    }
    
    /**
     * Returns the calendar ID.
     */
    public String getCalendarId() {
        return calendarId;
    }
    
    /**
     * Returns the application name.
     */
    public String getApplicationName() {
        return applicationName;
    }
    
    /**
     * Checks whether the configuration is complete and valid.
     */
    public boolean isValid() {
        return serviceAccountFile != null && !serviceAccountFile.isEmpty()
                && calendarId != null && !calendarId.isEmpty()
                && applicationName != null && !applicationName.isEmpty();
    }
    
    @Override
    public String toString() {
        return "CalendarConfig{" +
                "serviceAccountFile='" + serviceAccountFile + '\'' +
                ", calendarId='" + calendarId + '\'' +
                ", applicationName='" + applicationName + '\'' +
                '}';
    }
}
