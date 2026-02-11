package app.dialogos.googlecalendar.plugin;


import com.clt.diamant.graph.Graph;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Node;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.xml.sax.SAXException;
import com.clt.diamant.WozInterface;
import com.clt.diamant.InputCenter;
import com.clt.diamant.ExecutionLogger;
import com.clt.diamant.Slot;
import com.clt.script.exp.*;
import com.clt.dialogos.plugin.PluginRuntime;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;


/**
 * GoogleCalendarNode - ABSTRACT BASE CLASS for all Google Calendar operations.
 * 
 * IMPORTANT: This class is NOT intended to be used directly!
 * It is an abstract base class for concrete nodes like:
 * ├─ CreateEventNode
 * ├─ UpdateEventNode
 * ├─ ListEventsNode
 * └─ DeleteEventNode
 * 
 * A Dialog OS user will NOT be able to see this class directly.
**/
public abstract class GoogleCalendarNode extends Node {


    public GoogleCalendarNode() {
        super();
        this.addEdge();  // Standard success edge
    }



    @Override
    public abstract Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger) 
            throws NodeExecutionException;



    /**
     * Retrieves the shared PluginRuntime.
     */
    protected GoogleCalendarPluginRuntime getPluginRuntime(WozInterface comm) 
        throws NodeExecutionException {
        try {
            PluginRuntime runtime = this.getPluginSettings(GoogleCalendarPlugin.class).getRuntime(comm);
            if (runtime instanceof GoogleCalendarPluginRuntime) {
                return (GoogleCalendarPluginRuntime) runtime;
            } else {
                throw new NodeExecutionException(this, 
                        "Plugin Runtime nicht verfügbar oder falsch konfiguriert");
            }
        } catch (Exception e) {
            throw new NodeExecutionException(this, 
                    "Fehler beim Zugriff auf Plugin Runtime: " + e.getMessage());
        }
    }


    /**
     * Retrieves the calendar configuration (Global Settings).
     */
    protected CalendarConfig getCalendarConfig(WozInterface comm) throws NodeExecutionException {
        try {
            return getPluginRuntime(comm).getCalendarConfig();
        } catch (NodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeExecutionException(this, 
                    "Fehler beim Laden der Kalender-Konfiguration: " + e.getMessage());
        }
    }


    /**
     * Retrieves the authenticated Calendar Service.
     */
    protected com.google.api.services.calendar.Calendar getCalendarService(WozInterface comm) 
            throws NodeExecutionException {
        try {
            return getPluginRuntime(comm).getCalendarService();
        } catch (NodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeExecutionException(this, 
                    "Fehler beim Zugriff auf Google Calendar Service: " + e.getMessage());
        }
    }


    @Override
    protected void writeAttributes(XMLWriter out, IdMap uid_map) {
        super.writeAttributes(out, uid_map);
        // Base class has no additional properties to save
        // Concrete nodes override and save their properties
    }
    
    protected void writeAttributeIfNotEmpty(XMLWriter out, String name, String value) {
        if (value != null && !value.isEmpty()) {
            Graph.printAtt(out, name, value);
        }
    }


    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) 
            throws SAXException {
        super.readAttribute(r, name, value, uid_map);
        // Base class has no additional properties to load
        // Concrete nodes override and load their properties
    }


    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Google Calendar Node"), BorderLayout.WEST);
        
        // Concrete nodes should override this method
        return panel;
    }


    public static Color getDefaultColor() {
        return new Color(100, 150, 200);  // Blue for Google Calendar
    }


    @Override
    public void writeVoiceXML(XMLWriter out, IdMap uid_map) {
        // Not relevant for Google Calendar
    }


    protected void setStringVariable(String variableName, String value) 
        throws NodeExecutionException {
        try {
            Graph graph = this. getGraph();
            Value stringValue = new com.clt.script.exp.values.StringValue(value);
            graph.setSlotValue(variableName, stringValue);
        } catch (Exception e) {
            throw new NodeExecutionException(this, 
                "Fehler beim Setzen von '" + variableName + "':  " + e.getMessage(), e);
        }
    }
    
    /**
     * Replaces ${variableName} with actual value from Graph 
     * @param input Input string (can be ${var} or direct value)
     * @param logger For logging (optional)
     * @return Value with replaced variables
     * @throws NodeExecutionException if variable does not exist
     */
    protected String evaluateVariable(String input, ExecutionLogger logger, WozInterface comm) 
            throws NodeExecutionException {
        if (input == null || input.isEmpty()) {
            return input;
        }


        // Pattern: ${variableName}
        String pattern = "\\$\\{([^}]+)\\}";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(input);


        StringBuffer result = new StringBuffer();
        boolean found = false;


        while (m.find()) {
            found = true;
            String variableName = m.group(1);  // e.g. "eventTitle" from "${eventTitle}"


            try {
                Graph graph = this.getGraph();
                List<Slot> variables = graph.getVariables();
                // Search for the variable with matching name
                Slot targetSlot = null;
                for (Slot slot : variables) {
                    if (slot.getName().equals(variableName)) {
                        targetSlot = slot;
                        break;
                    }
                }
                if (targetSlot == null) {
                    m.appendReplacement(result, "");
                } else {
                    // Get the value of the variable
                    Value value = targetSlot.getValue();
                    if (value == null) {
                        m.appendReplacement(result, "");
                    } else {
                        String stringValue = value.toString();
                        m.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(stringValue));
                    }
                }


            } catch (NodeExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new NodeExecutionException(this,
                        "Fehler beim Zugriff auf Variable '${" + variableName + "}': " + 
                        e.getMessage());
            }
        }


        m.appendTail(result);
        
        // If no variable found: input remains as is
        return found ? result.toString() : input;
    }


    /**
     * Parses a DateTime string in ISO 8601 format.
     * 
     * @param dateTimeStr String in format "2025-01-15T10:00:00"
     * @param fieldName Name of the field for error messages
     * @return LocalDateTime
     * @throws NodeExecutionException on parse error
     */
    protected LocalDateTime parseDateTime(String dateTimeStr, String fieldName) 
            throws NodeExecutionException {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            throw new NodeExecutionException(this, fieldName + " ist erforderlich");
        }
        // Remove quotes (double and single) at start and end
        dateTimeStr = dateTimeStr.replaceAll("^[\"']+|[\"']+$", "");
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (DateTimeParseException e) {
            throw new NodeExecutionException(this,
                    fieldName + " hat ungültiges Format. Verwende ISO 8601: 2025-01-15T10:00:00\n" +
                    "Eingabe war: " + dateTimeStr);
        }
    }

    /**
     * Parses a JSON string containing dateTime and timeZone fields
     * and converts it to a LocalDateTime object.
     * 
     * @param jsonString JSON string with format: {"dateTime":"2026-01-15T10:00:00.000+01:00","timeZone":"UTC"}
     * @return LocalDateTime representation of the dateTime field
     */
    public LocalDateTime parseJsonToLocalDateTime(String jsonString) {
        // Parse JSON string to extract the dateTime value
        JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
        String dateTimeString = jsonObject.get("dateTime").getAsString();
        
        // Parse the ISO 8601 formatted string with offset to OffsetDateTime
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        // Convert to LocalDateTime (removes timezone/offset information)
        return offsetDateTime.toLocalDateTime();
    }



    /**
     * Parses reminder string and adds them to the builder.
     * 
     * Format: "method:minutes,method:minutes,..."
     * Example: "email:15,popup:30" → Email reminder 15min before, popup 30min before
     * 
     * @param builder EventRequest Builder
     * @param remindersStr Reminder string
     * @throws NodeExecutionException on parse error
     */
    protected void parseAndAddReminders(EventRequest.Builder builder, String remindersStr)
            throws NodeExecutionException {
        try {
            String[] reminderPairs = remindersStr.split(",");


            for (String pair : reminderPairs) {
                String[] parts = pair.trim().split(":");
                if (parts.length != 2) {
                    throw new IllegalArgumentException(
                            "Ungültiges Reminder-Format: '" + pair + 
                            "'. Verwende 'method:minutes' (z.B. 'email:15')");
                }


                String method = parts[0].trim();
                int minutes = Integer.parseInt(parts[1].trim());


                // Validate method
                if (!method.equals("email") && !method.equals("popup") && !method.equals("sms")) {
                    throw new IllegalArgumentException(
                            "Reminder-Methode '" + method + 
                            "' ungültig. Verwende: email, popup oder sms");
                }


                builder.addReminder(method, minutes);
            }
        } catch (NumberFormatException e) {
            throw new NodeExecutionException(this,
                    "Reminder-Minuten müssen Zahlen sein: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new NodeExecutionException(this, e.getMessage());
        }
    }


    // Helper method to get variable list
    protected String[] getListVariables() {
        List<String> varNames = new ArrayList<>();
        varNames.add("eventId"); // Default
        
        // Get all available variables from the graph
        Graph graph = this.getGraph();
        if (graph != null) {
            List<Slot> variables = graph.getVariables();
            for (Slot slot : variables) {
                if (slot.getType() == Type.String) {
                    varNames.add(slot.getName());
                }
            }
        }
        
        return varNames.toArray(new String[0]);
    }
}
