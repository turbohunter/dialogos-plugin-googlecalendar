# Google Calendar Plugin for DialogOS - System Architecture & Documentation

## 📋 Table of Contents
1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Plugin Components](#plugin-components)
4. [Node Reference](#node-reference)
5. [Configuration](#configuration)
6. [Usage Examples](#usage-examples)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Overview

The **Google Calendar Plugin** enables DialogOS to interact with Google Calendar through voice-based dialogs. It provides four main operations: creating, updating, listing, and deleting calendar events. The plugin uses Google's Calendar API with service account authentication.

### Key Features
- ✅ Create calendar events with reminders and locations
- ✅ Update existing events
- ✅ List events with multiple filtering modes (upcoming, time range, search, all)
- ✅ Delete events with notification options
- ✅ Variable-based input/output for dynamic dialogs
- ✅ Service account authentication for secure access

---

## System Architecture

The plugin follows a clean, modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                      DialogOS Framework                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│              GoogleCalendarPlugin (Entry Point)                  │
│  - Registers nodes with DialogOS                                 │
│  - Provides plugin metadata (ID, name, version)                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
┌───────▼──────────┐              ┌──────────▼─────────────┐
│ PluginSettings   │              │    PluginRuntime       │
│ (Configuration)  │              │  (API Management)      │
│                  │              │                        │
│ • Service Acct   │◄─────────────┤ • Authenticates        │
│ • Calendar ID    │              │ • Provides Service     │
│ • App Name       │              │ • Resource Mgmt        │
└──────────────────┘              └────────┬───────────────┘
                                           │
                   ┌───────────────────────┴───────────────────────┐
                   │         Google Calendar Service               │
                   │         (Authenticated API Client)            │
                   └───────────────────────┬───────────────────────┘
                                           │
        ┌──────────────────────────────────┼──────────────────────────────┐
        │                                  │                              │
┌───────▼────────┐  ┌──────────────────▼──────────┐  ┌─────────────▼────────┐
│ GoogleCalendar │  │   Abstract Base Class        │  │  Helper Classes      │
│   Nodes        │  │  (GoogleCalendarNode)        │  │                      │
│                │  │                              │  │ • CalendarConfig     │
│ • CreateEvent  │◄─┤ • getCalendarService()       │  │ • EventRequest       │
│ • UpdateEvent  │  │ • getCalendarConfig()        │  │ • EventConverter     │
│ • ListEvents   │  │ • evaluateVariable()         │  │                      │
│ • DeleteEvent  │  │ • parseDateTime()            │  │                      │
└────────────────┘  └──────────────────────────────┘  └──────────────────────┘
```

### Architecture Layers

#### 1. **Plugin Layer** (`GoogleCalendarPlugin`)
- **Purpose**: Entry point and registration
- **Responsibilities**:
  - Register all node types with DialogOS
  - Provide plugin metadata (ID, name, version)
  - Create default settings instance

#### 2. **Configuration Layer** (`GoogleCalendarPluginSettings`)
- **Purpose**: Global settings management
- **Responsibilities**:
  - Store service account file path
  - Store calendar ID
  - Store application name
  - Persist settings to/from XML
  - Provide UI editor for settings

#### 3. **Runtime Layer** (`GoogleCalendarPluginRuntime`)
- **Purpose**: API initialization and lifecycle management
- **Responsibilities**:
  - Authenticate with Google using service account
  - Initialize and manage Calendar Service instance
  - Validate configuration
  - Dispose resources on shutdown

#### 4. **Node Layer** (All Calendar Nodes)
- **Purpose**: Execute specific calendar operations
- **Responsibilities**:
  - Provide UI for node configuration
  - Execute calendar operations (CRUD)
  - Handle variable evaluation
  - Manage node properties (save/load)

#### 5. **Model Layer** (Helper Classes)
- **Purpose**: Data structures and utilities
- **Components**:
  - `CalendarConfig`: Immutable configuration holder
  - `EventRequest`: Builder-pattern for event data
  - `EventConverter`: Conversion between internal and Google API formats

---

## Plugin Components

### Core Classes

#### **GoogleCalendarPlugin**
```java
public class GoogleCalendarPlugin implements Plugin
```
**Purpose**: Plugin entry point that registers nodes with DialogOS

**Key Methods**:
- `getId()`: Returns `"dialogos.plugin.googlecalendar"`
- `getName()`: Returns `"Google Calendar"`
- `initialize()`: Registers all node types (Create, Update, List, Delete)
- `createDefaultSettings()`: Creates settings instance

---

#### **GoogleCalendarNode** (Abstract Base Class)
```java
public abstract class GoogleCalendarNode extends Node
```
**Purpose**: Base class for all calendar nodes with shared functionality

**Key Methods**:
- `getPluginRuntime(WozInterface)`: Access to plugin runtime
- `getCalendarConfig(WozInterface)`: Access to global configuration
- `getCalendarService(WozInterface)`: Access to authenticated Calendar API
- `evaluateVariable(String, ExecutionLogger, WozInterface)`: Variable substitution (`${varName}`)
- `parseDateTime(String, String)`: Parse ISO 8601 datetime strings
- `parseAndAddReminders(Builder, String)`: Parse reminder format (`email:15,popup:30`)
- `setStringVariable(String, String)`: Store result in DialogOS variable

**Shared Functionality**:
- Variable evaluation: Replaces `${variableName}` with actual values
- DateTime parsing: Handles ISO 8601 format (`2026-01-15T10:00:00`)
- Error handling: Consistent exception messages
- XML persistence: Save/load node properties

---

#### **GoogleCalendarPluginSettings**
```java
public class GoogleCalendarPluginSettings extends PluginSettings
```
**Purpose**: Global settings shared across all nodes

**Properties**:
- `serviceAccountFile`: Path to Google service account JSON file
- `calendarId`: Google Calendar ID (email format)
- `applicationName`: Application name for API requests

**Features**:
- UI editor with file browser for service account selection
- XML persistence (save/load settings with DialogOS project)
- Default values for quick setup

---

#### **GoogleCalendarPluginRuntime**
```java
public class GoogleCalendarPluginRuntime implements PluginRuntime
```
**Purpose**: Manages Google Calendar API lifecycle

**Responsibilities**:
1. **Authentication**: Loads service account credentials from JSON file
2. **API Initialization**: Creates authenticated Calendar service
3. **Validation**: Ensures all required settings are present
4. **Resource Management**: Disposes connections on shutdown

**Key Methods**:
- `initialize()`: Sets up Google Calendar API connection
- `getCalendarService()`: Provides Calendar service to nodes
- `getCalendarConfig()`: Provides configuration to nodes
- `dispose()`: Cleanup on shutdown

---

#### **CalendarConfig** (Immutable)
```java
public final class CalendarConfig
```
**Purpose**: Immutable configuration container for thread-safety

**Fields**:
- `serviceAccountFile`: Path to credentials
- `calendarId`: Target calendar
- `applicationName`: App identifier

---

#### **EventRequest** (Builder Pattern)
```java
public class EventRequest
```
**Purpose**: Structured event data before Google API conversion

**Fields**:
- `summary` (required): Event title
- `description` (optional): Event details
- `location` (optional): Event location
- `startTime` (required): Start date/time
- `endTime` (required): End date/time
- `reminders` (optional): List of event reminders

**Usage**:
```java
EventRequest event = EventRequest.builder()
    .summary("Team Meeting")
    .startTime(LocalDateTime.parse("2026-01-15T10:00:00"))
    .endTime(LocalDateTime.parse("2026-01-15T11:00:00"))
    .addReminder("email", 15)
    .build();
```

---

#### **EventConverter**
```java
public class EventConverter
```
**Purpose**: Convert between internal and Google API formats

**Key Methods**:
- `toGoogleCalendarEvent(EventRequest)`: Convert to Google Event
- `toEventRequest(Event)`: Convert from Google Event
- `createEventDateTime(LocalDateTime)`: Convert to Google DateTime
- `googleDateTimeToLocalDateTime(EventDateTime)`: Convert from Google DateTime
- `eventToString(Event)`: Human-readable event representation

---

## Node Reference

### 1. CreateEventNode

**Purpose**: Create new calendar events

**Properties**:
| Property | Type | Required | Description | Example |
|----------|------|----------|-------------|---------|
| `summary` | String | ✅ Yes | Event title | `"${eventTitle}"` or `"Team Meeting"` |
| `description` | String | ❌ No | Event details | `"${eventDesc}"` |
| `location` | String | ❌ No | Event location | `"Conference Room A"` |
| `startTime` | DateTime | ✅ Yes | Start time (ISO 8601) | `"2026-01-15T10:00:00"` or `"${startDateTime}"` |
| `endTime` | DateTime | ✅ Yes | End time (ISO 8601) | `"2026-01-15T11:00:00"` or `"${endDateTime}"` |
| `reminders` | String | ❌ No | Reminder list | `"email:15,popup:30"` |
| `resultVariable` | Variable | ✅ Yes | Output variable | `eventId` (stores created event ID) |

**Behavior**:
1. Evaluates all variables (`${varName}` → actual value)
2. Validates required fields (summary, startTime, endTime)
3. Parses datetime strings (ISO 8601 format)
4. Creates event via Google Calendar API
5. Stores resulting event ID in `resultVariable`

**Example Configuration**:
```
Summary: ${eventTitle}
Start Time: ${startDateTime}
End Time: ${endDateTime}
Location: Conference Room A
Reminders: email:15,popup:30
Result Variable: eventId
```

**Output**: Stores event ID (e.g., `"abc123def456"`) in selected variable

---

### 2. UpdateEventNode

**Purpose**: Update existing calendar events

**Properties**:
| Property | Type | Required | Description | Example |
|----------|------|----------|-------------|---------|
| `eventId` | String | ✅ Yes | ID of event to update | `"${eventId}"` or `"abc123def456"` |
| `summary` | String | ✅ Yes | New title | `"${newTitle}"` or `"Updated Meeting"` |
| `description` | String | ❌ No | New description | `"${newDesc}"` |
| `location` | String | ❌ No | New location | `"${newLocation}"` |
| `startTime` | DateTime | ✅ Yes | New start time | `"2026-01-16T14:00:00"` |
| `endTime` | DateTime | ✅ Yes | New end time | `"2026-01-16T15:00:00"` |
| `reminders` | String | ❌ No | New reminders | `"email:30"` |
| `resultVariable` | Variable | ✅ Yes | Output variable | `updatedEventId` |

**Behavior**:
1. Evaluates variables including event ID
2. Updates only provided fields (partial update supported)
3. Sends notifications to all participants
4. Stores updated event ID in result variable

**Example Configuration**:
```
Event ID: ${eventId}
Summary: ${newTitle}
Start Time: 2026-01-16T14:00:00
End Time: 2026-01-16T15:00:00
Result Variable: updatedEventId
```

---

### 3. ListEventsNode

**Purpose**: List calendar events with various filtering options

**List Modes**:
1. **UPCOMING**: Events from now onwards
2. **TIME_RANGE**: Events within specific date/time range
3. **SEARCH**: Events matching a search query
4. **ALL**: All events (limited by maxResults)

**Properties**:
| Property | Type | Required | Description | Example |
|----------|------|----------|-------------|---------|
| `listMode` | Enum | ✅ Yes | Listing mode | `UPCOMING`, `TIME_RANGE`, `SEARCH`, `ALL` |
| `searchQuery` | String | For SEARCH | Search term | `"Team Meeting"` or `"${searchTerm}"` |
| `startTime` | DateTime | For TIME_RANGE | Range start | `"2026-01-15T00:00:00"` |
| `endTime` | DateTime | For TIME_RANGE | Range end | `"2026-01-16T00:00:00"` |
| `maxResults` | Integer | ❌ No | Max events (default: 10) | `"20"` or `"${maxEvents}"` |
| `resultVariable` | Variable | ✅ Yes | Output variable | `eventList` |

**Behavior**:
1. Executes selected list mode operation
2. Formats results as multi-line string:
   ```
   ID | Title | Start | End
   abc123 | Meeting 1 | 2026-01-15T10:00:00 | 2026-01-15T11:00:00
   def456 | Meeting 2 | 2026-01-15T14:00:00 | 2026-01-15T15:00:00
   ```
3. Stores formatted string in result variable

**Example Configurations**:

**Upcoming Events**:
```
List Mode: UPCOMING
Max Results: 10
Result Variable: eventList
```

**Time Range**:
```
List Mode: TIME_RANGE
Start Time: 2026-01-15T00:00:00
End Time: 2026-01-16T00:00:00
Max Results: 20
Result Variable: eventList
```

**Search**:
```
List Mode: SEARCH
Search Query: Team Meeting
Max Results: 5
Result Variable: eventList
```

---

### 4. DeleteEventNode

**Purpose**: Delete calendar events

**Properties**:
| Property | Type | Required | Description | Example |
|----------|------|----------|-------------|---------|
| `eventId` | String | ✅ Yes | ID of event to delete | `"${eventId}"` or `"abc123def456"` |
| `sendUpdates` | Enum | ❌ No | Notification mode | `all`, `externalOnly`, `none` |
| `resultVariable` | Variable | ✅ Yes | Output variable | `deletionResult` |

**Send Updates Options**:
- `all`: Notify all participants
- `externalOnly`: Notify only external (non-organization) participants
- `none`: Don't send notifications

**Behavior**:
1. Evaluates event ID variable
2. Deletes event from calendar
3. Sends notifications based on `sendUpdates` setting
4. Stores confirmation message in result variable

**Example Configuration**:
```
Event ID: ${eventId}
Send Updates: all
Result Variable: deletionResult
```

**Output**: Stores message like:
```
"Event deleted successfully: abc123def456 (participants notified)"
```

---

## Configuration

### Global Plugin Settings

Access via: **DialogOS → Preferences → Plugins → Google Calendar**

#### Required Settings:

1. **Service Account File**
   - Path to Google service account JSON file
   - Example: `/path/to/service-account-key.json`
   - Get from: Google Cloud Console → IAM & Admin → Service Accounts

2. **Calendar ID**
   - Google Calendar identifier (usually email format)
   - Example: `your-calendar@gmail.com`
   - Find in: Google Calendar → Settings → Calendar settings

3. **Application Name**
   - Identifier for your DialogOS application
   - Example: `"DialogOS Calendar Assistant"`
   - Used in API requests for tracking

### Setting Up Google Calendar API

#### Prerequisites:
- Google account (free Gmail account sufficient)
- Google Cloud Console access (requires credit card verification for free tier)

#### Steps:
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable Google Calendar API
4. Create Service Account:
   - IAM & Admin → Service Accounts → Create Service Account
   - Grant "Calendar API" permissions
   - Create JSON key and download
5. Share your calendar with the service account email
6. Configure plugin with downloaded JSON file path and calendar ID

#### Limitations:
- Service accounts cannot add attendees to events
- Service accounts cannot create Google Meet links
- Events are created as "busy" by default

---

## Usage Examples

### Example 1: Simple Event Creation

**Dialog Flow**:
```
User: "Create a meeting for tomorrow at 2 PM"
Bot: (extracts time and title)
     → CreateEventNode
Bot: "Meeting created with ID: abc123"
```

**Node Configuration**:
```
CreateEventNode:
  summary: ${extractedTitle}
  startTime: ${extractedStartTime}
  endTime: ${extractedEndTime}
  resultVariable: eventId
```

---

### Example 2: List and Update Events

**Dialog Flow**:
```
User: "What meetings do I have tomorrow?"
Bot: → ListEventsNode (TIME_RANGE mode)
Bot: "You have 2 meetings: Team Standup at 9 AM, Review at 2 PM"
User: "Change the review to 3 PM"
Bot: (extracts event ID and new time)
     → UpdateEventNode
Bot: "Updated successfully"
```

**Node Configurations**:
```
ListEventsNode:
  listMode: TIME_RANGE
  startTime: ${tomorrowStart}
  endTime: ${tomorrowEnd}
  resultVariable: eventList

UpdateEventNode:
  eventId: ${selectedEventId}
  startTime: 2026-01-16T15:00:00
  endTime: 2026-01-16T16:00:00
  resultVariable: updateResult
```

---

### Example 3: Search and Delete

**Dialog Flow**:
```
User: "Delete all meetings with John"
Bot: → ListEventsNode (SEARCH mode)
Bot: "Found 3 meetings with John"
User: "Delete the first one"
Bot: → DeleteEventNode
Bot: "Meeting deleted and John was notified"
```

**Node Configurations**:
```
ListEventsNode:
  listMode: SEARCH
  searchQuery: John
  maxResults: 10
  resultVariable: foundEvents

DeleteEventNode:
  eventId: ${firstEventId}
  sendUpdates: all
  resultVariable: deleteConfirmation
```

---

## Best Practices

### 1. Variable Naming Conventions
- Use descriptive names: `eventId`, `meetingTitle`, `startDateTime`
- Prefix related variables: `create_eventId`, `update_eventId`
- Keep output variables consistent across nodes

### 2. DateTime Handling
- Always use ISO 8601 format: `YYYY-MM-DDTHH:mm:ss`
- Example: `2026-01-15T10:00:00`
- Variables work: `${startDateTime}` must contain valid ISO 8601 string
- Avoid ambiguous formats

### 3. Error Handling
- Check result variables for success
- Use conditional edges based on results
- Handle missing/invalid event IDs gracefully

### 4. Security
- **Never commit service account JSON files to version control**
- Store credentials in secure location
- Use environment-specific configuration
- Regularly rotate service account keys

### 5. Performance
- Limit `maxResults` to reasonable values (10-50)
- Use specific list modes (TIME_RANGE, SEARCH) over ALL
- Cache event IDs instead of repeated searches

### 6. Reminders Format
- Use consistent format: `method:minutes`
- Multiple reminders: `email:15,popup:30,sms:60`
- Valid methods: `email`, `popup`, `sms`
- Minutes: positive integers

### 7. Testing
- Test with test calendar first
- Verify service account permissions
- Check time zone handling
- Validate variable substitution

---

## Troubleshooting

### Common Issues

**"Service Account File path is not set"**
- Solution: Configure global plugin settings with valid JSON file path

**"Calendar ID is not set"**
- Solution: Add calendar ID in plugin settings (usually email format)

**"Failed to initialize Google Calendar API"**
- Check: JSON file exists and is readable
- Check: Service account has calendar access
- Check: Calendar is shared with service account email

**"Event ID is required"**
- Solution: Ensure `eventId` variable is set before Update/Delete nodes
- Check: Previous List/Create node stored ID correctly

**"Invalid format. Use ISO 8601"**
- Solution: Use format `YYYY-MM-DDTHH:mm:ss`
- Example: `2026-01-15T10:00:00`
- Check: Variables contain valid datetime strings

**"Variable not found"**
- Solution: Define variable in graph before using in node
- Check: Variable name matches exactly (case-sensitive)
- Check: `${variableName}` syntax is correct

---

## Summary

The Google Calendar Plugin provides a complete solution for voice-based calendar management in DialogOS:

- **4 Node Types**: Create, Update, List, Delete
- **Clean Architecture**: Separation of concerns with shared base class
- **Flexible Configuration**: Global settings + per-node properties
- **Variable Support**: Dynamic dialogs with `${variable}` syntax
- **Multiple List Modes**: Upcoming, time range, search, all
- **Builder Pattern**: Type-safe event construction
- **Error Handling**: Comprehensive validation and error messages
- **Thread-Safe**: Immutable configuration objects

Perfect for building voice assistants that manage calendars, schedule meetings, and interact with Google Calendar through natural language dialogs.

---

**Plugin Version**: 1.0.0  
**DialogOS Compatibility**: DialogOS Framework  
**Google Calendar API**: v3  
**Authentication**: Service Account (OAuth 2.0)
