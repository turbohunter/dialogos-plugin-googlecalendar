package app.dialogos.googlecalendar.plugin;

import com.clt.diamant.graph.Graph;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Node;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.diamant.gui.NodePropertiesDialog;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.xml.sax.SAXException;
import com.clt.diamant.WozInterface;
import com.clt.diamant.InputCenter;
import com.clt.diamant.ExecutionLogger;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;
import java.awt.*;

/**
 * ListEventsNode - Node for listing Google Calendar Events.
 * 
 * Supported List Modes:
 * 1. UPCOMING: Events from now onwards (limited by maxResults)
 * 2. TIME_RANGE: Events within a specific date/time range
 * 3. SEARCH: Events matching a search query
 * 4. ALL: All events (limited by maxResults)
 * 
 * Required Properties:
 * - listMode (mandatory - UPCOMING, TIME_RANGE, SEARCH, or ALL)
 * - searchQuery (required for SEARCH mode)
 * - startTime (required for TIME_RANGE mode)
 * - endTime (required for TIME_RANGE mode)
 * - maxResults (optional - default: 10)
 * - resultVariable (output: list of events as formatted string or error)
 * 
 * Global settings (serviceAccountFile, calendarId, etc.)
 * come from GoogleCalendarPluginSettings!
 */
public class ListEventsNode extends GoogleCalendarNode {

    private static final String PROP_LIST_MODE = "listMode";
    private static final String PROP_SEARCH_QUERY = "searchQuery";
    private static final String PROP_START_TIME = "startTime";
    private static final String PROP_END_TIME = "endTime";
    private static final String PROP_MAX_RESULTS = "maxResults";
    private static final String PROP_RESULT_VAR = "resultVariable";

    // List mode constants
    private static final String MODE_UPCOMING = "UPCOMING";
    private static final String MODE_TIME_RANGE = "TIME_RANGE";
    private static final String MODE_SEARCH = "SEARCH";
    private static final String MODE_ALL = "ALL";

    public ListEventsNode() {
        super();
        this.setProperty(PROP_LIST_MODE, MODE_UPCOMING);
        this.setProperty(PROP_SEARCH_QUERY, "");
        this.setProperty(PROP_START_TIME, "");
        this.setProperty(PROP_END_TIME, "");
        this.setProperty(PROP_MAX_RESULTS, "10");
        this.setProperty(PROP_RESULT_VAR, "eventList");
    }

    @Override
    public Node execute(WozInterface comm, InputCenter input, ExecutionLogger logger)
            throws NodeExecutionException {
        try {
            System.out.println("=== ListEventsNode Execute ===");

            String listMode = getProperty(PROP_LIST_MODE).toString();
            String maxResultsStr = evaluateVariable(
                    getProperty(PROP_MAX_RESULTS).toString(), logger, comm);
            String resultVariable = evaluateVariable(
                    getProperty(PROP_RESULT_VAR).toString(), logger, comm);

            // Parse max results
            int maxResults = 10; // default
            try {
                maxResults = Integer.parseInt(maxResultsStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid maxResults format, using default: 10");
            }

            CalendarConfig config = getCalendarConfig(comm);
            Calendar service = getCalendarService(comm);

            List<Event> events = new ArrayList<>();

            // Execute based on list mode
            switch (listMode) {
                case MODE_UPCOMING:
                    events = listUpcomingEvents(service, config.getCalendarId(), maxResults);
                    System.out.println("Listed " + events.size() + " upcoming events");
                    break;

                case MODE_TIME_RANGE:
                    String startTimeInput = evaluateVariable(
                            getProperty(PROP_START_TIME).toString(), logger, comm);
                    String endTimeInput = evaluateVariable(
                            getProperty(PROP_END_TIME).toString(), logger, comm);

                    if (startTimeInput == null || startTimeInput.isEmpty()) {
                        throw new NodeExecutionException(this,
                                "Start Time is required for TIME_RANGE mode");
                    }
                    if (endTimeInput == null || endTimeInput.isEmpty()) {
                        throw new NodeExecutionException(this,
                                "End Time is required for TIME_RANGE mode");
                    }

                    LocalDateTime startTime = parseDateTime(startTimeInput, "Start Time");
                    LocalDateTime endTime = parseDateTime(endTimeInput, "End Time");

                    events = listEventsByTimeRange(service, config.getCalendarId(),
                            startTime, endTime, maxResults);
                    System.out.println("Listed " + events.size() + " events in time range");
                    break;

                case MODE_SEARCH:
                    String searchQuery = evaluateVariable(
                            getProperty(PROP_SEARCH_QUERY).toString(), logger, comm);

                    if (searchQuery == null || searchQuery.isEmpty()) {
                        throw new NodeExecutionException(this,
                                "Search Query is required for SEARCH mode");
                    }

                    events = searchEvents(service, config.getCalendarId(), searchQuery, maxResults);
                    System.out.println("Found " + events.size() + " events matching: " + searchQuery);
                    break;

                case MODE_ALL:
                    events = listAllEvents(service, config.getCalendarId(), maxResults);
                    System.out.println("Listed " + events.size() + " total events");
                    break;

                default:
                    throw new NodeExecutionException(this, "Unknown list mode: " + listMode);
            }

            // Format events for output
            String formattedEvents = formatEventsAsJson(events, maxResults);
            setStringVariable(resultVariable, formattedEvents);
            System.out.println("formatted events: " + formattedEvents);
            System.out.println("Events stored in variable: " + resultVariable);

            return this.getEdge(0).getTarget();

        } catch (NodeExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeExecutionException(this,
                    "Error listing events: " + e.getMessage(), e);
        }
    }

    /**
     * Lists upcoming events from now onwards
     */
    private List<Event> listUpcomingEvents(Calendar service, String calendarId, int maxResults)
            throws Exception {
        DateTime now = new DateTime(System.currentTimeMillis());

        Events events = service.events().list(calendarId)
                .setTimeMin(now)
                .setMaxResults(maxResults)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems() != null ? events.getItems() : new ArrayList<>();
    }

    /**
     * Lists events within a specific date/time range
     */
    private List<Event> listEventsByTimeRange(Calendar service, String calendarId,
            LocalDateTime start, LocalDateTime end, int maxResults)
            throws Exception {
        DateTime startTime = toDateTime(start);
        DateTime endTime = toDateTime(end);

        Events events = service.events().list(calendarId)
                .setTimeMin(startTime)
                .setTimeMax(endTime)
                .setMaxResults(maxResults)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems() != null ? events.getItems() : new ArrayList<>();
    }

    /**
     * Searches events by query string
     */
    private List<Event> searchEvents(Calendar service, String calendarId, String query, int maxResults)
            throws Exception {
        Events events = service.events().list(calendarId)
                .setQ(query)
                .setMaxResults(maxResults)
                .execute();

        return events.getItems() != null ? events.getItems() : new ArrayList<>();
    }

    /**
     * Lists all events (limited by maxResults)
     */
    private List<Event> listAllEvents(Calendar service, String calendarId, int maxResults)
            throws Exception {
        Events events = service.events().list(calendarId)
                .setMaxResults(maxResults)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        return events.getItems() != null ? events.getItems() : new ArrayList<>();
    }

    /**
     * Converts LocalDateTime to Google DateTime
     */
    private DateTime toDateTime(LocalDateTime localDateTime) {
        long millis = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new DateTime(millis);
    }

    /**
     * Formats list of events into readable string representation
     * Each event on new line: "ID | Title | Start | End"
     */
    private String formatEventsList(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return "No events found";
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            String id = event.getId();
            String summary = event.getSummary() != null ? event.getSummary() : "(No title)";
            String start = event.getStart() != null ? event.getStart().toString() : "N/A";
            String end = event.getEnd() != null ? event.getEnd().toString() : "N/A";

            result.append(id).append(" | ")
                    .append(summary).append(" | ")
                    .append(start).append(" | ")
                    .append(end);

            if (i < events.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private String formatEventsAsJson(List<Event> events, int displayCount) {
        int startIndex = 0;

        // Configure ObjectMapper with JavaTimeModule for LocalDateTime support
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Map<String, Object> response = new HashMap<>();
        
        if (events == null || events.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("total_count", 0);
            metadata.put("displayed_count", 0);
            metadata.put("start_index", 0);
            metadata.put("has_more", false);
            response.put("metadata", metadata);
            response.put("events", new ArrayList<>());
            
            try {
                return mapper.writeValueAsString(response);
            } catch (Exception e) {
                return "{\"error\":\"No events found\"}";
            }
        }

        // metadata for pagination
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("total_count", events.size());
        metadata.put("displayed_count", Math.min(displayCount, events.size()));
        metadata.put("start_index", startIndex);
        metadata.put("has_more", (startIndex + displayCount) < events.size());
        
        response.put("metadata", metadata);
        
        // events with structured data
        List<Map<String, Object>> eventsList = new ArrayList<>();
        for (int i = startIndex; i < Math.min(startIndex + displayCount, events.size()); i++) {
            Event event = events.get(i);
            Map<String, Object> eventMap = new HashMap<>();
            
            LocalDateTime start_time = parseJsonToLocalDateTime(event.getStart().toString());
            LocalDateTime end_time = parseJsonToLocalDateTime(event.getEnd().toString());

            eventMap.put("index", i + 1);
            eventMap.put("id", event.getId());
            eventMap.put("summary", event.getSummary() != null ? event.getSummary() : "(No title)");
            eventMap.put("start", start_time);
            eventMap.put("end", end_time);
            eventMap.put("duration_minutes", calculateDuration(start_time, end_time));
            eventMap.put("location", event.getLocation());
            eventMap.put("description", event.getDescription());
            
            eventsList.add(eventMap);
        }
        
        response.put("events", eventsList);
        
        try {
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            System.out.println("JSON serialization failed: " + e + " using fallback");
            return formatEventsList(events); // Fallback
        }
    }

    private int calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        return (int) java.time.temporal.ChronoUnit.MINUTES.between(start, end);
    }

    @Override
    protected void writeAttributes(XMLWriter out, IdMap uid_map) {
        super.writeAttributes(out, uid_map);

        Graph.printAtt(out, PROP_LIST_MODE, this.getProperty(PROP_LIST_MODE).toString());
        Graph.printAtt(out, PROP_SEARCH_QUERY, this.getProperty(PROP_SEARCH_QUERY).toString());
        Graph.printAtt(out, PROP_START_TIME, this.getProperty(PROP_START_TIME).toString());
        Graph.printAtt(out, PROP_END_TIME, this.getProperty(PROP_END_TIME).toString());
        Graph.printAtt(out, PROP_MAX_RESULTS, this.getProperty(PROP_MAX_RESULTS).toString());
        Graph.printAtt(out, PROP_RESULT_VAR, this.getProperty(PROP_RESULT_VAR).toString());
    }

    @Override
    protected void readAttribute(XMLReader r, String name, String value, IdMap uid_map)
            throws SAXException {
        super.readAttribute(r, name, value, uid_map);

        if (PROP_LIST_MODE.equals(name)) {
            setProperty(PROP_LIST_MODE, value);
        } else if (PROP_SEARCH_QUERY.equals(name)) {
            setProperty(PROP_SEARCH_QUERY, value);
        } else if (PROP_START_TIME.equals(name)) {
            setProperty(PROP_START_TIME, value);
        } else if (PROP_END_TIME.equals(name)) {
            setProperty(PROP_END_TIME, value);
        } else if (PROP_MAX_RESULTS.equals(name)) {
            setProperty(PROP_MAX_RESULTS, value);
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
                "LIST MODES:\n" +
                "UPCOMING    - Events from now onwards\n" +
                "TIME_RANGE  - Events in specific date range\n" +
                "SEARCH      - Events matching a query\n" +
                "ALL         - All events\n\n" +
                "VARIABLE SYNTAX:\n" +
                "${variableName} - value of variable\n" +
                "DateTime Format: 2026-01-15T10:00:00\n"
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(new Color(240, 240, 240));
        inputPanel.add(new JScrollPane(infoArea));

        // Ensure all properties exist as strings
        properties.putIfAbsent(PROP_LIST_MODE, this.getProperty(PROP_LIST_MODE));
        properties.putIfAbsent(PROP_SEARCH_QUERY, this.getProperty(PROP_SEARCH_QUERY));
        properties.putIfAbsent(PROP_START_TIME, this.getProperty(PROP_START_TIME));
        properties.putIfAbsent(PROP_END_TIME, this.getProperty(PROP_END_TIME));
        properties.putIfAbsent(PROP_MAX_RESULTS, this.getProperty(PROP_MAX_RESULTS));
        properties.putIfAbsent(PROP_RESULT_VAR, this.getProperty(PROP_RESULT_VAR));

        // List Mode (MANDATORY)
        JPanel modeRow = new JPanel();
        JLabel modeLabel = new JLabel("List Mode (REQUIRED):");
        modeLabel.setForeground(Color.RED);
        modeRow.add(modeLabel);
        String[] modes = {MODE_UPCOMING, MODE_TIME_RANGE, MODE_SEARCH, MODE_ALL};
        JComboBox<String> modeCombo = NodePropertiesDialog.createComboBox(
                properties, PROP_LIST_MODE, modes);
        modeCombo.setToolTipText("Select listing mode");
        modeRow.add(modeCombo);
        inputPanel.add(modeRow);

        // Search Query (for SEARCH mode)
        JPanel searchRow = new JPanel();
        searchRow.add(new JLabel("Search Query:"));
        JTextField searchField = NodePropertiesDialog.createTextField(properties, PROP_SEARCH_QUERY);
        searchField.setToolTipText("e.g. 'Meeting', 'Team' - required for SEARCH mode");
        searchRow.add(searchField);
        inputPanel.add(searchRow);

        // Start Time (for TIME_RANGE mode)
        JPanel startRow = new JPanel();
        startRow.add(new JLabel("Start Time:"));
        JTextField startField = NodePropertiesDialog.createTextField(properties, PROP_START_TIME);
        startField.setToolTipText("e.g. '2026-01-15T10:00:00' - required for TIME_RANGE mode");
        startRow.add(startField);
        inputPanel.add(startRow);

        // End Time (for TIME_RANGE mode)
        JPanel endRow = new JPanel();
        endRow.add(new JLabel("End Time:"));
        JTextField endField = NodePropertiesDialog.createTextField(properties, PROP_END_TIME);
        endField.setToolTipText("e.g. '2026-01-15T18:00:00' - required for TIME_RANGE mode");
        endRow.add(endField);
        inputPanel.add(endRow);

        // Max Results
        JPanel maxRow = new JPanel();
        maxRow.add(new JLabel("Max Results:"));
        JTextField maxField = NodePropertiesDialog.createTextField(properties, PROP_MAX_RESULTS);
        maxField.setToolTipText("Default: 10");
        maxRow.add(maxField);
        inputPanel.add(maxRow);

        // Result Variable - as ComboBox
        JPanel resultRow = new JPanel();
        resultRow.add(new JLabel("Result Variable:"));
        JComboBox<?> resultCombo = NodePropertiesDialog.createComboBox(
                properties,
                PROP_RESULT_VAR,
                getListVariables()
        );
        resultCombo.setToolTipText("Output: formatted event list will be stored here");
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
