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
import com.clt.script.exp.Value;
import com.clt.script.exp.*;
import com.clt.diamant.WozInterface;
import com.clt.diamant.InputCenter;
import com.clt.diamant.ExecutionLogger;
import com.clt.diamant.Slot;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import app.dialogos.googlecalendar.plugin.CalendarConfig;
import app.dialogos.googlecalendar.plugin.EventRequest;
import app.dialogos.googlecalendar.plugin.EventConverter;

import org.xml.sax.SAXException;

import java.util.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import java.awt.*;


/**
 * UpdateEventNode - Node for updating existing Google Calendar Events.
 * 
 * Required Properties:
 * - eventId (mandatory - ID of the event to update)
 * - eventTitle/summary (optional - new title)
 * - eventDescription (optional - new description)
 * - startTime (optional - new start time)
 * - endTime (optional - new end time)
 * - eventLocation (optional - new location)
 * - reminders (optional - new reminders)
 * - resultVariable (output: updated event ID or error)
 * 
 * Global settings (serviceAccountFile, calendarId, etc.) 
 * come from GoogleCalendarPluginSettings!
 */
public class UpdateEventNode extends GoogleCalendarNode {


    private static final String PROP_EVENT_ID = "eventId";
    private static final String PROP_SUMMARY = "summary";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_LOCATION = "eventlocation";
    private static final String PROP_START_TIME = "startTime";
    private static final String PROP_END_TIME = "endTime";
    private static final String PROP_REMINDERS = "reminders";
    private static final String PROP_RESULT_VAR = "resultVariable";


    public UpdateEventNode() {
        super();
        this.setProperty(PROP_EVENT_ID, "");
        this.setProperty(PROP_SUMMARY, "");
        this.setProperty(PROP_DESCRIPTION, "");
        this.setProperty(PROP_LOCATION, "");
        this.setProperty(PROP_START_TIME, "");
        this.setProperty(PROP_END_TIME, "");
        this.setProperty(PROP_REMINDERS, "");
        this.setProperty(PROP_RESULT_VAR, "eventId");
    }


    @Override
    public Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger) 
            throws NodeExecutionException {
        try {
            String eventIdInput = getProperty(PROP_EVENT_ID).toString();
            System.out.println("=== UpdateEventNode Execute ===");
            
            // Evaluate eventId - this is mandatory
            String eventId = evaluateVariable(eventIdInput, logger, comm).replaceAll("^[\"']+|[\"']+$", "");
            if (eventId == null || eventId.isEmpty()) {
                throw new NodeExecutionException(this, "Event ID is required");
            }
            System.out.println("Event ID evaluation: " + eventId);
            
            String summaryInput = evaluateVariable(
                getProperty(PROP_SUMMARY).toString(), logger, comm);
            String descriptionInput = evaluateVariable(
                getProperty(PROP_DESCRIPTION).toString(), logger, comm);
            String locationInput = evaluateVariable(
                getProperty(PROP_LOCATION).toString(), logger, comm);
            String startTimeInput = evaluateVariable(
                getProperty(PROP_START_TIME).toString(), logger, comm);
            String endTimeInput = evaluateVariable(
                getProperty(PROP_END_TIME).toString(), logger, comm);
            String remindersInput = evaluateVariable(
                getProperty(PROP_REMINDERS).toString(), logger, comm);
            String resultVariable = evaluateVariable(
                getProperty(PROP_RESULT_VAR).toString(), logger, comm);
            
            // Parse and validate optional time parameters if provided
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            
            if (startTimeInput != null && !startTimeInput.isEmpty()) {
                startTime = parseDateTime(startTimeInput, "Start Time");
            }
            if (endTimeInput != null && !endTimeInput.isEmpty()) {
                endTime = parseDateTime(endTimeInput, "End Time");
            }

            // Construct EventRequest with only the fields that should be updated
            EventRequest.Builder eventBuilder = EventRequest.builder();
            
            if (summaryInput != null && !summaryInput.isEmpty()) {
                eventBuilder.summary(summaryInput);
            }
            if (descriptionInput != null && !descriptionInput.isEmpty()) {
                eventBuilder.description(descriptionInput);
            }
            if (locationInput != null && !locationInput.isEmpty()) {
                eventBuilder.location(locationInput);
            }
            if (startTime != null && endTime != null) {
                eventBuilder.startTime(startTime);
                eventBuilder.endTime(endTime);
            }
            if (remindersInput != null && !remindersInput.isEmpty()) {
                parseAndAddReminders(eventBuilder, remindersInput);
            }
            
            // validates required Fields
            EventRequest eventRequest = eventBuilder.build();

            Event event = EventConverter.toGoogleCalendarEvent(eventRequest);
            System.out.println("Sending event: " +
             "id: " + event.getId() + "\n" +
             "summary: " + event.getSummary() + "\n" +
             "start: " + event.getStart() + "\n" +
             "end: " + event.getEnd() + "\n"
             );
            CalendarConfig config = getCalendarConfig(comm);
            Calendar service = getCalendarService(comm);


            // Update the event in Google Calendar
            Event updatedEvent = service.events().update(
                    config.getCalendarId(),
                    eventId,
                    event
            )
            .setSendUpdates("all")  // Notify all participants about the update
            .execute();


            // Store result in output variable
            String resultAlias = getProperty(PROP_RESULT_VAR).toString();
            setStringVariable(resultAlias, updatedEvent.getId());
            System.out.println("Event updated: " + updatedEvent.getId() + 
                             " (" + (summaryInput != null ? summaryInput : "unchanged") + ")");
            
            return this.getEdge(0).getTarget();

        } catch (NodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeExecutionException(this, 
                    "Error updating event: " + e.getMessage(), e);
        }
    }
    

    @Override
    protected void writeAttributes(XMLWriter out, IdMap uid_map) {
        super.writeAttributes(out, uid_map);
        
        Graph.printAtt(out, PROP_EVENT_ID, this.getProperty(PROP_EVENT_ID).toString());
        Graph.printAtt(out, PROP_SUMMARY, this.getProperty(PROP_SUMMARY).toString());
        Graph.printAtt(out, PROP_DESCRIPTION, this.getProperty(PROP_DESCRIPTION).toString());
        Graph.printAtt(out, PROP_LOCATION, this.getProperty(PROP_LOCATION).toString());
        Graph.printAtt(out, PROP_START_TIME, this.getProperty(PROP_START_TIME).toString());
        Graph.printAtt(out, PROP_END_TIME, this.getProperty(PROP_END_TIME).toString());
        Graph.printAtt(out, PROP_REMINDERS, this.getProperty(PROP_REMINDERS).toString());
        Graph.printAtt(out, PROP_RESULT_VAR, this.getProperty(PROP_RESULT_VAR).toString());
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map) 
            throws SAXException {
        super.readAttribute(r, name, value, uid_map);
        
        if (PROP_EVENT_ID.equals(name)) {
            setProperty(PROP_EVENT_ID, value);
        } else if (PROP_SUMMARY.equals(name)) {
            setProperty(PROP_SUMMARY, value);
        } else if (PROP_DESCRIPTION.equals(name)) {
            setProperty(PROP_DESCRIPTION, value);
        } else if (PROP_LOCATION.equals(name)) {
            setProperty(PROP_LOCATION, value);
        } else if (PROP_START_TIME.equals(name)) {
            setProperty(PROP_START_TIME, value);
        } else if (PROP_END_TIME.equals(name)) {
            setProperty(PROP_END_TIME, value);
        } else if (PROP_REMINDERS.equals(name)) {
            setProperty(PROP_REMINDERS, value);
        } else if (PROP_RESULT_VAR.equals(name)) {
            setProperty(PROP_RESULT_VAR, value);
        }
        
    }       

    @Override
    public JComponent createEditorComponent(Map<String, Object> properties) {
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

        // Info Panel
        JTextArea infoArea = new JTextArea(
                "VARIABLE SYNTAX:\n" +
                "${eventId} - value of variable 'eventId' (REQUIRED)\n" +
                "or direct value\n" +
                "DateTime Format: 2026-01-15T10:00:00" +  "\n" + 
                "result variable can be chosen from exisiting variables"
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(new Color(240, 240, 240));
        inputPanel.add(new JScrollPane(infoArea));

        // Ensure all properties exist as strings
        properties.putIfAbsent(PROP_EVENT_ID, this.getProperty(PROP_EVENT_ID));
        properties.putIfAbsent(PROP_SUMMARY, this.getProperty(PROP_SUMMARY));
        properties.putIfAbsent(PROP_DESCRIPTION, this.getProperty(PROP_DESCRIPTION));
        properties.putIfAbsent(PROP_LOCATION, this.getProperty(PROP_LOCATION));
        properties.putIfAbsent(PROP_START_TIME, this.getProperty(PROP_START_TIME));
        properties.putIfAbsent(PROP_END_TIME, this.getProperty(PROP_END_TIME));
        properties.putIfAbsent(PROP_REMINDERS, this.getProperty(PROP_REMINDERS));
        properties.putIfAbsent(PROP_RESULT_VAR, this.getProperty(PROP_RESULT_VAR));

        // Event ID (MANDATORY - highlighted)
        JPanel eventIdRow = new JPanel();
        JLabel eventIdLabel = new JLabel("Event ID (REQUIRED):");
        eventIdLabel.setForeground(Color.RED);
        eventIdRow.add(eventIdLabel);
        JTextField eventIdField = NodePropertiesDialog.createTextField(properties, PROP_EVENT_ID);
        eventIdField.setToolTipText("e.g. '${selectedEventId}' or direct event ID string");
        eventIdRow.add(eventIdField);
        inputPanel.add(eventIdRow);
        
        // Summary 
        JPanel summaryRow = new JPanel();
        JLabel summaryLabel = new JLabel("Summary (REQUIRED):");
        summaryLabel.setForeground(Color.RED);
        summaryRow.add(summaryLabel);
        JTextField summaryField = NodePropertiesDialog.createTextField(properties, PROP_SUMMARY);
        summaryField.setToolTipText("e.g. '${eventTitle}' or 'Updated Meeting'");
        summaryRow.add(summaryField);
        inputPanel.add(summaryRow);

        // Description 
        JPanel descRow = new JPanel();
        descRow.add(new JLabel("Description:"));
        JTextField descField = NodePropertiesDialog.createTextField(properties, PROP_DESCRIPTION);
        descField.setToolTipText("e.g. '${eventDesc}'");
        descRow.add(descField);
        inputPanel.add(descRow);

        // Location 
        JPanel locRow = new JPanel();
        locRow.add(new JLabel("Location:"));
        JTextField locField = NodePropertiesDialog.createTextField(properties, PROP_LOCATION);
        locField.setToolTipText("e.g. '${eventLocation}'");
        locRow.add(locField);
        inputPanel.add(locRow);

        // Start Time
        JPanel startRow = new JPanel();
        JLabel startLabel = new JLabel("Start Time (REQUIRED):");
        startLabel.setForeground(Color.RED);
        startRow.add(startLabel);
        JTextField startField = NodePropertiesDialog.createTextField(properties, PROP_START_TIME);
        startField.setToolTipText("z.B. '${startDateTime}' or '2025-01-15T10:00:00'");
        startRow.add(startField);
        inputPanel.add(startRow);

        // End Time
        JPanel endRow = new JPanel();
        JLabel endLabel = new JLabel("End Time (REQUIRED):");
        endLabel.setForeground(Color.RED);
        endRow.add(endLabel);
        JTextField endField = NodePropertiesDialog.createTextField(properties, PROP_END_TIME);
        endField.setToolTipText("z.B. '${endDateTime}'");
        endRow.add(endField);
        inputPanel.add(endRow);

        // Reminders 
        JPanel remindersRow = new JPanel();
        remindersRow.add(new JLabel("Reminders :"));
        JTextField remindersField = NodePropertiesDialog.createTextField(properties, PROP_REMINDERS);
        remindersField.setToolTipText("Format: email:15,popup:30");
        remindersRow.add(remindersField);
        inputPanel.add(remindersRow);

        // Result Variable - as ComboBox
        JPanel resultRow = new JPanel();
        resultRow.add(new JLabel("Result Variable:"));
        JComboBox<?> resultCombo = NodePropertiesDialog.createComboBox(
            properties,
            PROP_RESULT_VAR,
            getListVariables()
        );
        resultCombo.setToolTipText("Output: updated Event ID will be stored here");
        resultRow.add(resultCombo);
        inputPanel.add(resultRow);

        mainPanel.add(new JScrollPane(inputPanel), BorderLayout.CENTER);
        return mainPanel;
    }

    @Override
    public void writeVoiceXML(XMLWriter out, IdMap uid_map) {
        // not relevant
    }
}
