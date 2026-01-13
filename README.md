# dialogos-plugin-googlecalendar
Control Google Calendar via Dialog OS


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
   - Example: `your-calendar@group.calendar.google.com`
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