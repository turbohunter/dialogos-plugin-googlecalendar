package app.dialogos.googlecalendar.plugin;

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
import com.google.api.services.calendar.Calendar;

import java.util.Map;
import javax.swing.*;
import java.awt.*;

/**
 * DeleteEventNode - Node for deleting Google Calendar Events.
 * 
 * Required Properties:
 * - eventId (mandatory - ID of the event to delete)
 * - sendUpdates (optional - notify participants: "all", "externalOnly", "none")
 * - resultVariable (output: confirmation message or error)
 * 
 * Global settings (serviceAccountFile, calendarId, etc.)
 * come from GoogleCalendarPluginSettings!
 */
public class DeleteEventNode extends GoogleCalendarNode {

    private static final String PROP_EVENT_ID = "eventId";
    private static final String PROP_SEND_UPDATES = "sendUpdates";
    private static final String PROP_RESULT_VAR = "resultVariable";

    // Send updates constants
    private static final String SEND_ALL = "all";
    private static final String SEND_EXTERNAL_ONLY = "externalOnly";
    private static final String SEND_NONE = "none";

    public DeleteEventNode() {
        super();
        this.setProperty(PROP_EVENT_ID, "");
        this.setProperty(PROP_SEND_UPDATES, SEND_ALL);
        this.setProperty(PROP_RESULT_VAR, "deletionResult");
    }

    @Override
    public Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger)
            throws NodeExecutionException {
        try {
            System.out.println("=== DeleteEventNode Execute ===");

            // Evaluate eventId - this is mandatory
            String eventIdInput = getProperty(PROP_EVENT_ID).toString();
            String eventId = evaluateVariable(eventIdInput, logger, comm).replaceAll("^[\"']+|[\"']+$", "");

            if (eventId == null || eventId.isEmpty()) {
                throw new NodeExecutionException(this, "Event ID is required");
            }

            System.out.println("Event ID to delete: " + eventId);

            String sendUpdatesMode = getProperty(PROP_SEND_UPDATES).toString();
            String resultVariable = evaluateVariable(
                    getProperty(PROP_RESULT_VAR).toString(), logger, comm);

            CalendarConfig config = getCalendarConfig(comm);
            Calendar service = getCalendarService(comm);

            // Delete the event from Google Calendar
            service.events().delete(config.getCalendarId(), eventId)
                    .setSendUpdates(sendUpdatesMode)
                    .execute();

            // Prepare result message
            String resultMessage = "Event deleted successfully: " + eventId;
            if (SEND_ALL.equals(sendUpdatesMode)) {
                resultMessage += " (participants notified)";
            } else if (SEND_NONE.equals(sendUpdatesMode)) {
                resultMessage += " (no notifications sent)";
            } else if (SEND_EXTERNAL_ONLY.equals(sendUpdatesMode)) {
                resultMessage += " (external participants notified)";
            }

            setStringVariable(resultVariable, resultMessage);
            System.out.println("✅ " + resultMessage);

            return this.getEdge(0).getTarget();

        } catch (NodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeExecutionException(this,
                    "Error deleting event: " + e.getMessage(), e);
        }
    }

    @Override
    protected void writeAttributes(XMLWriter out, IdMap uid_map) {
        super.writeAttributes(out, uid_map);

        Graph.printAtt(out, PROP_EVENT_ID, this.getProperty(PROP_EVENT_ID).toString());
        Graph.printAtt(out, PROP_SEND_UPDATES, this.getProperty(PROP_SEND_UPDATES).toString());
        Graph.printAtt(out, PROP_RESULT_VAR, this.getProperty(PROP_RESULT_VAR).toString());
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map)
            throws SAXException {
        super.readAttribute(r, name, value, uid_map);

        if (PROP_EVENT_ID.equals(name)) {
            setProperty(PROP_EVENT_ID, value);
        } else if (PROP_SEND_UPDATES.equals(name)) {
            setProperty(PROP_SEND_UPDATES, value);
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
                "DELETE EVENT SETTINGS:\n" +
                "send updates options:\n" +
                "all \n" +
                "externalOnly \n" +
                "none  \n\n" +
                "VARIABLE SYNTAX:\n" +
                "${eventId} - value of variable 'eventId' (REQUIRED)\n" +
                "or direct value"
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(new Color(240, 240, 240));
        inputPanel.add(new JScrollPane(infoArea));

        // Ensure all properties exist as strings
        properties.putIfAbsent(PROP_EVENT_ID, this.getProperty(PROP_EVENT_ID));
        properties.putIfAbsent(PROP_SEND_UPDATES, this.getProperty(PROP_SEND_UPDATES));
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

        // Send Updates - as ComboBox
        JPanel sendUpdatesRow = new JPanel();
        sendUpdatesRow.add(new JLabel("Send Updates:"));
        String[] updateOptions = {SEND_ALL, SEND_EXTERNAL_ONLY, SEND_NONE};
        JComboBox<String> sendUpdatesCombo = NodePropertiesDialog.createComboBox(
                properties, PROP_SEND_UPDATES, updateOptions);
        sendUpdatesCombo.setToolTipText("Choose how to notify participants");
        sendUpdatesRow.add(sendUpdatesCombo);
        inputPanel.add(sendUpdatesRow);

        // Result Variable - as ComboBox
        JPanel resultRow = new JPanel();
        resultRow.add(new JLabel("Result Variable:"));
        JComboBox<?> resultCombo = NodePropertiesDialog.createComboBox(
                properties,
                PROP_RESULT_VAR,
                getListVariables()
        );
        resultCombo.setToolTipText("Output: deletion confirmation message will be stored here");
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
