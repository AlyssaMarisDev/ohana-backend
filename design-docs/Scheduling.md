# App Design Document

## Overview
This document outlines the design and features of the app, which aims to facilitate social interactions and event management among users. The app provides functionalities for account creation, social networking, event scheduling, group management, calendar integration, and payment handling.

## Features

### 1. Account Creation
- Users can create an account using their email
- Profile setup with personal information and preferences, including:
  - **Name**: Full name of the user.
  - **Profile Picture**: Upload a profile picture to personalize the account.
  - **Age**:
  - **Pronouns**:
  - **Birthday**:
  - **Location**: User's current city or location.
  - **Contact Information**: Optional phone number or alternative contact methods.
  - **Notification Preferences**: Settings to manage how and when the user receives notifications.

### 2. Friending Other People
- Users can search for and send friend requests to other users.
- Accept or decline friend requests.
- View a list of friends and their profiles.

### 3. Scheduling Events with Friends
- Create and manage events with friends.
- Set event details such as date, time, location, and description.
- Invite friends to events and track RSVPs.

### 4. Creating Groups
- Users can create groups for specific interests (e.g., hiking, foodie, DnD).
- Add or remove members from groups.
- Manage group settings and privacy.

### 5. Creating Events for Groups
- Schedule events specifically for groups.
- Notify group members of new events.
- Track group event participation.

### 6. Google Calendar Integration
- Connect to Google Calendar for reading and writing events.
- Sync app events with Google Calendar.
- Check for conflicts with existing calendar events.

### 7. Conflict Warning
- Warn users if a scheduled event conflicts with another event on their or a friend's calendar.
- Provide options to reschedule or notify affected parties.

### 8. Venmo Integration
- Integrate with Venmo for cost splitting among event participants.
- Generate Venmo payment links with pre-filled amounts and recipient details.
- Allow users to send and receive payments through Venmo.

### 9. Messaging
- **Individual Messaging**: Allow users to send direct messages to each other.
  - Text-based communication with options for emojis and attachments.
  - Notification settings for new messages.
- **Group Messaging**: Enable messaging within groups.
  - Create group chats for event planning and discussions.
  - Manage group chat settings and permissions.
  - Support for multimedia messages and file sharing.

## Technical Considerations
- **Authentication**: Implement secure authentication mechanisms for account creation and login.
- **Data Storage**: Use a scalable database to store user data, events, and group information.
- **API Integration**: Utilize APIs for Google Calendar and Venmo integration.
- **User Interface**: Design a user-friendly interface for easy navigation and interaction.
- **Security**: Ensure data privacy and protection, especially for financial transactions.

## Conclusion
This app aims to enhance social connectivity and event management by providing a comprehensive set of features that cater to users' social and organizational needs. The integration with popular services like Google Calendar and Venmo further enriches the user experience by offering seamless scheduling and payment solutions.
