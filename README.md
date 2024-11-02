## Overview

**PulseStep** is a fitness app designed for users who want to track workouts and stay motivated. The app has features for running and jump rope tracking, with various data metrics and a social leaderboard to help users see their progress.

---

## Installation and Setup

### Requirements
- **Android Version**: Android 5.0 or higher
- **Google Maps API Key**: Add this in the `local.properties` file:
  ```properties
  MAPS_API_KEY=your_api_key_here
  ```

### Installation Steps
1. Clone this repository.
2. Add the Google Maps API key in the `local.properties` file:
   ```properties
   MAPS_API_KEY=your_api_key_here
   ```
3. Configure the API key in `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="${MAPS_API_KEY}" />
   ```
4. Install dependencies, build, and run the app.

---

## Features

### 1. Sign-Up and Sign-In

- **Sign-Up**: New users create an account by entering name, birthdate, height, weight, and gender, then verify by email.
- **Sign-In**: Existing users log in with email and password. If you forget your password, use the "Forgot Password" option to reset it via email.

### 2. Workout Modes

- **Running**: Tracks distance, pace, time, steps, and calories burned. Requires GPS to show your path.
- **Jump Rope**: Counts jumps, pace, time, and calories burned. Music control is available for both workout modes.
- **Upload Data**: After finishing a workout, tap "End" to save data to Firebase.

### 3. Social Leaderboard and Interaction

- **Daily and Monthly Leaderboards**: Set daily goals, view your rank, and interact with other users.
- **Minimum Time for Leaderboard**: Workouts must be **over 5 minutes** to appear on the leaderboard.
- **Likes and Notifications**: Like other users’ achievements. Notifications appear in the notification center.

### 4. Notification Settings

- **In-App Notifications**: You can turn on or off notifications like likes and leaderboard updates in the settings menu.

### 5. Profile Management

- **Profile Settings**: Change your profile picture from your gallery or camera. Update height and weight for accurate calorie tracking.
- **View Workout Data**: Check your daily workout history with a built-in calendar (view up to 15 days back and 7 days ahead).

### 6. App Settings

- **Location Settings**: Turn location tracking on or off.
- **Change Password**: Update your password anytime for account security.
- **User Guide**: Find instructions on using the app.
- **Account Management**: Log out or deactivate your account if you wish.

---

## Usage Steps

### 1. Network and GPS Check
- The app checks for network connection and GPS at startup. If either is off, the app may not work correctly.

### 2. Location Permission and Map Mode
- After starting, the app asks for location permission:
  - **Allow**: Enables map mode with live tracking.
  - **Deny**: Switches to no-map mode, and location features are disabled.

### 3. Main Page Workout Selection
- The main page has options for Running and Jump Rope. Before you start, the app will ask for "Activity Recognition" permission:
  - **Allow**: Full tracking for workouts is enabled.
  - **Deny**: Running and Jump Rope tracking will be unavailable.

### 4. Background Permission for Continuous Tracking
- After starting a workout, the app asks for background permission:
  - **Allow**: The app keeps tracking, even if the screen is locked or the app is minimized.
  - **Deny**: Tracking stops if you leave the app or lock the screen.

---

## Important Notes

For the best experience, make sure to turn on network and GPS before using the app. All permissions are only used for tracking workouts; the app does not collect extra information without permission.

---

## Dependencies

PulseStep uses the following main libraries:

- **AndroidX**: Fragment, AppCompat, Lifecycle, ConstraintLayout
- **Google Play Services**: Location and Maps
- **Firebase**: Authentication, Firestore Database, and Storage
- **UI Components**: Material Design, Glide, Image Picker, CircleImageView, GIF support
- **Testing**: JUnit, Espresso

These libraries ensure smooth UI, accurate location tracking, Firebase integration, and testing support.

---

## Tested Devices

PulseStep has been tested on:

- Xiaomi 14 Ultra
- Xiaomi 13
- Google Pixel 8 Pro (API 35)

---

## License

PulseStep is licensed under the [MIT License](LICENSE).
