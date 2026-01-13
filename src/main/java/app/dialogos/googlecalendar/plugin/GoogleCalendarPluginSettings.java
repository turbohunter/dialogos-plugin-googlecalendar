package app.dialogos.googlecalendar.plugin;

import com.clt.diamant.*;
import com.clt.dialogos.plugin.PluginRuntime;
import com.clt.dialogos.plugin.PluginSettings;
import com.clt.dialogos.plugin.Plugin;
import com.clt.diamant.graph.Node;
import com.clt.diamant.IdMap;
import com.clt.diamant.graph.Graph;
import com.clt.diamant.graph.nodes.NodeExecutionException;
import com.clt.diamant.gui.NodePropertiesDialog;
import com.google.auth.http.HttpCredentialsAdapter;
import com.clt.properties.*;
import com.clt.xml.XMLReader;
import com.clt.xml.XMLWriter;

import org.xml.sax.SAXException;

import java.awt.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * GoogleCalendarPluginSettings - Verwaltet globale Einstellungen für alle Google Calendar Nodes.
 * Diese Klasse speichert die gemeinsamen Konfigurationswerte, die über alle Nodes hinweg gelten.
 * 
 * Struktur:
 * - Global Settings (PluginSettings): serviceAccountFile, calendarId, applicationName
 * - Pro-Node Settings: werden in den einzelnen Node-Klassen verwaltet
 */
public class GoogleCalendarPluginSettings extends PluginSettings {
    
    static final String DEFAULT_SERVICE_ACCOUNT_FILE = "service_account.json";
    StringProperty serviceAccountFileProperty = new DefaultStringProperty(
            "SERVICE_ACCOUNT_FILE", null, null,
            DEFAULT_SERVICE_ACCOUNT_FILE) {
        @Override
        public String getName() {
            return "Service Account File";
        }
        @Override
        public String getDescription() {
            return "Path to the Google service account JSON file";
        }
    };

    static final String DEFAULT_CALENDAR_ID = "";
    StringProperty calendarIdProperty = new DefaultStringProperty(
            "CALENDAR_ID", null, null,
            DEFAULT_CALENDAR_ID) {
        @Override
        public String getName() {
            return "Calendar ID";
        }
        @Override
        public String getDescription() {
            return "Google Calendar ID";
        }
    };

    static final String DEFAULT_APPLICATION_NAME = "";
    StringProperty applicationNameProperty = new DefaultStringProperty(
            "APPLICATION_NAME", null, null,
            DEFAULT_APPLICATION_NAME) {
        @Override
        public String getName() {
            return "Application Name";
        }
        @Override
        public String getDescription() {
            return "Name of the application using this calendar";
        }
    };
    
    /**
     * Gibt die globale Kalender-Konfiguration zurück.
     * Diese wird von allen Nodes verwendet.
     */
    public CalendarConfig getCalendarConfig() {
        return new CalendarConfig(
                serviceAccountFileProperty.getValue(),
                calendarIdProperty.getValue(),
                applicationNameProperty.getValue()
        );
    }

    public String getServiceAccountFile() {
        return serviceAccountFileProperty.getValue();
    }

    public String getCalendarId() {
        return calendarIdProperty.getValue();
    }

    public String getApplicationName() {
        return applicationNameProperty.getValue();
    }

    // Setter für externe Konfiguration
    public void setServiceAccountFile(String path) {
        this.serviceAccountFileProperty.setValue(path);
    }

    public void setCalendarId(String calendarId) {
        this.calendarIdProperty.setValue(calendarId);
    }

    public void setApplicationName(String appName) {
        this.applicationNameProperty.setValue(appName);
    }

    @Override
    public void writeAttributes(XMLWriter xmlWriter, IdMap idMap) {
        if (!serviceAccountFileProperty.getValue().equals(DEFAULT_SERVICE_ACCOUNT_FILE))
            Graph.printAtt(xmlWriter, serviceAccountFileProperty.getID(), serviceAccountFileProperty.getValue());
        if (!calendarIdProperty.getValue().equals(DEFAULT_CALENDAR_ID))
            Graph.printAtt(xmlWriter, calendarIdProperty.getID(), calendarIdProperty.getValue());
        if (!applicationNameProperty.getValue().equals(DEFAULT_APPLICATION_NAME))
            Graph.printAtt(xmlWriter, applicationNameProperty.getID(), applicationNameProperty.getValue());
    }

   @Override
    protected void readAttribute(XMLReader xmlReader, String name, String value, IdMap idMap) throws SAXException {
        if (name.equals(serviceAccountFileProperty.getID())) {
            serviceAccountFileProperty.setValue(value);
        } else if (name.equals(calendarIdProperty.getID())) {
            calendarIdProperty.setValue(value);
        } else if (name.equals(applicationNameProperty.getID())) {
            applicationNameProperty.setValue(value);
        }
    }

    @Override
    protected PluginRuntime createRuntime(Component component) {
        return new GoogleCalendarPluginRuntime(this);
    }

    @Override
    public JComponent createEditor() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Panel für die Settings
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Service Account File mit Dateiauswahl
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        settingsPanel.add(new JLabel("Service Account File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel filePanel = new JPanel(new BorderLayout());
        JTextField fileField = new JTextField(getServiceAccountFile(), 30);
        fileField.setEditable(false);
        filePanel.add(fileField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
            int result = fileChooser.showOpenDialog(mainPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                setServiceAccountFile(selectedFile.getAbsolutePath());
                fileField.setText(selectedFile.getAbsolutePath());
            }
        });
        filePanel.add(browseButton, BorderLayout.EAST);
        settingsPanel.add(filePanel, gbc);

        // Calendar ID
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        settingsPanel.add(new JLabel("Calendar ID:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField calendarIdField = new JTextField(getCalendarId(), 30);
        calendarIdField.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offset, String str, javax.swing.text.AttributeSet attr) 
                    throws javax.swing.text.BadLocationException {
                super.insertString(offset, str, attr);
                setCalendarId(getText(0, getLength()));
            }
        });
        settingsPanel.add(calendarIdField, gbc);

        // Application Name
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        settingsPanel.add(new JLabel("Application Name:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField appNameField = new JTextField(getApplicationName(), 30);
        appNameField.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offset, String str, javax.swing.text.AttributeSet attr) 
                    throws javax.swing.text.BadLocationException {
                super.insertString(offset, str, attr);
                setApplicationName(getText(0, getLength()));
            }
        });
        settingsPanel.add(appNameField, gbc);

        mainPanel.add(settingsPanel);
        mainPanel.add(Box.createVerticalGlue());

        // make sure values are set after components are created
        SwingUtilities.invokeLater(() -> {
            fileField.setText(getServiceAccountFile() != null ? getServiceAccountFile() : "");
            calendarIdField.setText(getCalendarId() != null ? getCalendarId() : "");
            appNameField.setText(getApplicationName() != null ? getApplicationName() : "");
        });
        // Panel mit Scroll-Support
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        return scrollPane;
    }
}
