package app.dialogos.googlecalendar.plugin;

/**
 * CalendarConfig - Immutable Konfigurationsklasse für Google Calendar.
 * 
 * Diese Klasse enthält alle globalen Einstellungen, die von allen Nodes gemeinsam
 * genutzt werden. Sie ist immutable (unveränderbar) für Thread-Safety.
 * 
 * Verwendung:
 * - Wird von GoogleCalendarPluginRuntime verwendet um die API zu initialisieren
 * - Wird von allen Calendar Nodes verwendet um auf Konfiguration zuzugreifen
 */
public final class CalendarConfig {
    
    private final String serviceAccountFile;
    private final String calendarId;
    private final String applicationName;
    
    /**
     * Erstellt eine neue Kalender-Konfiguration.
     * 
     * @param serviceAccountFile Absoluter Pfad zur Google Service Account JSON Datei
     * @param calendarId Google Calendar ID
     * @param applicationName Name der Anwendung für Google API Requests
     */
    public CalendarConfig(String serviceAccountFile, String calendarId, String applicationName) {
        this.serviceAccountFile = serviceAccountFile;
        this.calendarId = calendarId;
        this.applicationName = applicationName;
    }
    
    /**
     * Gibt den Pfad zur Service Account Datei zurück.
     */
    public String getServiceAccountFile() {
        return serviceAccountFile;
    }
    
    /**
     * Gibt die Calendar ID zurück.
     */
    public String getCalendarId() {
        return calendarId;
    }
    
    /**
     * Gibt den Namen der Anwendung zurück.
     */
    public String getApplicationName() {
        return applicationName;
    }
    
    /**
     * Prüft ob die Konfiguration vollständig und valide ist.
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