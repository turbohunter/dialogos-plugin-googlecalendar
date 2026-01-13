package app.dialogos.googlecalendar.plugin;


import com.clt.dialogos.plugin.PluginSettings;
import com.clt.diamant.graph.Node;


import com.clt.gui.Images;
import java.util.Arrays;
import javax.swing.Icon;


/**
 * Google Calendar Plugin for DialogOS
 * 
 * Registers all Calendar Nodes with the DialogOS system
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
        // here the nodes are registered
        Node.registerNodeTypes(
            this.getName(),  // group name in the UI
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
