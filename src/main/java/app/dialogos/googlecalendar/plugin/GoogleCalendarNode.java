package app.dialogos.googlecalendar.plugin;

import com.clt.diamant.*;
import com.clt.diamant.graph.Graph;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Node;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.diamant.gui.NodePropertiesDialog;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import org.xml.sax.SAXException;
import com.clt.diamant.WozInterface;
import com.clt.diamant.InputCenter;
import com.clt.diamant.ExecutionLogger;
import com.clt.diamant.Slot;
import com.google.api.client.util.DateTime;
import com.clt.script.exp.*;
import com.clt.diamant.Slot;
import com.google.api.services.calendar.Calendar;
import com.clt.dialogos.plugin.PluginRuntime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import app.dialogos.googlecalendar.plugin.CalendarConfig;
import app.dialogos.googlecalendar.plugin.GoogleCalendarPluginRuntime;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Arrays;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * GoogleCalendarNode - ABSTRAKTE BASE-KLASSE für alle Google Calendar Operationen.
 * 
 * WICHTIG: Diese Klasse ist NICHT dazu bestimmt direkt verwendet zu werden!
 * Sie ist eine abstrakte Basis-Klasse für konkrete Nodes wie:
 * ├─ CreateEventNode
 * ├─ UpdateEventNode
 * ├─ ListEventsNode
 * └─ DeleteEventNode
 * 
 * Ein Dialog OS Nutzer wird diese Klasse NICHT direkt sehen können.
**/
public abstract class GoogleCalendarNode extends Node {

    public GoogleCalendarNode() {
        super();
        this.addEdge();  // Standard Success-Kante
    }


    @Override
    public abstract Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger) 
            throws NodeExecutionException;


    /**
     * Ruft die gemeinsame PluginRuntime ab.
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
     * Ruft die Kalender-Konfiguration ab (Global Settings).
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
     * Ruft den authentifizierten Calendar Service ab.
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
        // Base-Klasse hat keine zusätzlichen Properties zu speichern
        // Konkrete Nodes überschreiben und speichern ihre Properties
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
        // Base-Klasse hat keine zusätzlichen Properties zu laden
        // Konkrete Nodes überschreiben und laden ihre Properties
    }

    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Google Calendar Node"), BorderLayout.WEST);
        
        // Konkrete Nodes sollten diese Methode überschreiben
        return panel;
    }

    public static Color getDefaultColor() {
        return new Color(100, 150, 200);  // Blau für Google Calendar
    }

    @Override
    public void writeVoiceXML(XMLWriter out, IdMap uid_map) {
        // Nicht relevant für Google Calendar
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
     * Ersetzt ${variableName} mit echtem Wert aus Graph 
     * @param input Input-String (kann ${var} oder direkt Wert sein)
     * @param logger Für Logging (optional)
     * @return Wert mit ersetzten Variablen
     * @throws NodeExecutionException wenn Variable nicht existiert
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
            String variableName = m.group(1);  // z.B. "eventTitle" aus "${eventTitle}"

            try {
                Graph graph = this.getGraph();
                List<Slot> variables = graph.getVariables();
                // Suche die Variable mit dem passenden Namen
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
                    // Hole den Wert der Variable
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
        
        // Wenn keine Variable gefunden: Input bleibt wie ist
        return found ? result.toString() : input;
    }

    /**
     * Parst einen DateTime-String im ISO 8601 Format.
     * 
     * @param dateTimeStr String im Format "2025-01-15T10:00:00"
     * @param fieldName Name des Feldes für Error-Messages
     * @return LocalDateTime
     * @throws NodeExecutionException bei Parse-Fehler
     */
    protected LocalDateTime parseDateTime(String dateTimeStr, String fieldName) 
            throws NodeExecutionException {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            throw new NodeExecutionException(this, fieldName + " ist erforderlich");
        }
        // Entfernt Anführungszeichen (doppelt und einfach) vorne und hinten
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
     * Parst Reminder-String und fügt sie zum Builder hinzu.
     * 
     * Format: "method:minutes,method:minutes,..."
     * Beispiel: "email:15,popup:30" → Email-Erinnerung 15min vor, Popup 30min vor
     * 
     * @param builder EventRequest Builder
     * @param remindersStr Reminder-String
     * @throws NodeExecutionException bei Parse-Fehler
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

                // Validiere Methode
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
