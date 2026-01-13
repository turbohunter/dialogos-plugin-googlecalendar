package app.dialogos.googlecalendar.plugin;

import com.clt.dialogos.plugin.PluginSettings;
import com.clt.dialogos.plugin.Plugin;
import com.clt.diamant.graph.Node;

import com.clt.gui.Images;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import javax.swing.Icon;

/**
 * Google Calendar Plugin für DialogOS
 * 
 * Registriert alle Calendar Nodes beim DialogOS-System
 */
public class GoogleCalendarPlugin implements com.clt.dialogos.plugin.Plugin {
    
    @Override
    public String getId() {
        return "dialogos.plugin.googlecalendar";
    }
    
    @Override
    public String getName() {
        return "Google Calendar";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public Icon getIcon() {
        return Images.load(this, "calendarIcon.png");
    }
    
    @Override
    public void initialize() {
        // hier werden die Nodes registriert
        Node.registerNodeTypes(
            this.getName(),  // Gruppen-Name im UI
            Arrays.asList(CreateEventNode.class, 
                UpdateEventNode.class, 
                ListEventsNode.class, 
                DeleteEventNode.class)
        );
    }

    @Override
    public PluginSettings createDefaultSettings() {
        return new GoogleCalendarPluginSettings();
    }
}