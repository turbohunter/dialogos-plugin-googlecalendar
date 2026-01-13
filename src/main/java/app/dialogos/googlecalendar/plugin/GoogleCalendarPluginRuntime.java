package app.dialogos.googlecalendar.plugin;


import com.clt.dialogos.plugin.PluginRuntime;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;


import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;


/**
 * GoogleCalendarPluginRuntime - Initializes and manages the Google Calendar API
 * based on the settings. This is a singleton-like structure per plugin.
 * 
 * Responsibilities:
 * - Authentication via Service Account
 * - Providing the Calendar Service for all Nodes
 * - Resource Management (connections, credentials)
 */
public class GoogleCalendarPluginRuntime implements PluginRuntime {


    private final GoogleCalendarPluginSettings settings;
    private Calendar calendarService;
    private GoogleCredentials credentials;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();


    public GoogleCalendarPluginRuntime(GoogleCalendarPluginSettings settings) {
        this.settings = settings;


        try {
            initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Google Calendar Plugin: " + e.getMessage(), e);
        }
    }


    public void initialize() throws Exception {
        try {
            CalendarConfig config = settings.getCalendarConfig();
            
            validateConfiguration(config);
            
            // Load credentials from the Service Account file
            this.credentials = GoogleCredentials.fromStream(
                    new FileInputStream(config.getServiceAccountFile())
            ).createScoped(Collections.singletonList(CalendarScopes.CALENDAR));
            
            // Create the Calendar Service
            this.calendarService = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(this.credentials))
            .setApplicationName(config.getApplicationName())
            .build();
            
            System.out.println("Google Calendar Plugin initialized successfully");
        } catch (IOException e) {
            throw new Exception("Failed to initialize Google Calendar API: " + e.getMessage(), e);
        } catch (GeneralSecurityException e) {
            throw new Exception("Security error during initialization: " + e.getMessage(), e);
        }
    }


    /**
     * Validates the configuration before initialization.
     */
    private void validateConfiguration(CalendarConfig config) throws IllegalArgumentException {
        if (config.getServiceAccountFile() == null || config.getServiceAccountFile().isEmpty()) {
            throw new IllegalArgumentException("Service Account File path is not set");
        }
        if (config.getCalendarId() == null || config.getCalendarId().isEmpty()) {
            throw new IllegalArgumentException("Calendar ID is not set");
        }
        if (config.getApplicationName() == null || config.getApplicationName().isEmpty()) {
            throw new IllegalArgumentException("Application Name is not set");
        }
    }
    
    /**
     * Returns the initialized Calendar Service.
     * Nodes use this method to access the API.
     */
    public Calendar getCalendarService() throws Exception {
        if (this.calendarService == null) {
            throw new Exception("Calendar Service not initialized. Call initialize() first.");
        }
        return this.calendarService;
    }


    /**
     * Returns the shared calendar configuration.
     */
    public CalendarConfig getCalendarConfig() {
        return settings.getCalendarConfig();
    }


    @Override
    public void dispose() {
        try {
            if (this.calendarService != null) {
                this.calendarService = null;
            }
            if (this.credentials != null) {
                this.credentials = null;
            }
            System.out.println("Google Calendar Plugin shut down");
        } catch (Exception e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }


    /**
     * Returns the settings (for node access).
     */
    public GoogleCalendarPluginSettings getSettings() {
        return settings;
    }
}
