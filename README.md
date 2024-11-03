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

### Build Configuration
- **Namespace**: `com.example.pulsestepapplication`
- **SDK Versions**:
  - **Compile SDK**: 34
  - **Min SDK**: 28
  - **Target SDK**: 34
- **Application ID**: `com.example.pulsestepapplication`

### Installation Steps
1. Clone this repository or Download the [app](./PulseStep_v1.apk).
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

### 4. Profile Management
- **Profile Settings**: Change your profile picture from your gallery or camera. Update height and weight for accurate calorie tracking.
- **View Workout Data**: Check your daily workout history with a built-in calendar (view up to 15 days back and 7 days ahead).

### 5. App Settings
- **Location Settings**: Turn location tracking on or off.
- **Change Password**: Update your password anytime for account security.
- **In-App Notifications**: You can turn on or off notifications like likes and leaderboard updates in the settings menu.
- **In-App Notifications**: You can turn on or off notifications like likes and leaderboard updates in the settings menu.
- **User Guide**: Find instructions on using the app.
- **Account Management**: Log out or deactivate your account if you wish.

---

## Sensors Used

PulseStep leverages multiple sensors for real-time tracking and user experience:

1. **Location Sensor (GPS)**:
   - Used for real-time location tracking and path mapping during workouts.
   - Integrated via `FusedLocationProviderClient` to capture GPS and network-based location data for accurate route tracking.

2. **Step Detector Sensor** (`TYPE_STEP_DETECTOR`):
   - Counts steps during running sessions, ensuring accurate measurement of the user's physical activity.
   - Requires **ACTIVITY_RECOGNITION** permission for step detection.

3. **Accelerometer Sensor** (`TYPE_ACCELEROMETER`):
   - Tracks jump rope activity by detecting changes in acceleration, counting jumps based on motion data.
   - Provides acceleration data along the x, y, and z axes, allowing the app to recognize specific motion patterns associated with jumps.

4. **Camera Sensor**:
   - Allows users to capture or upload a profile picture.
   - Integrated through **ImagePicker**, enabling users to take photos or select images from the gallery.

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

---

## Team Members

| <img src="https://avatars.githubusercontent.com/u/133179618?s=64&v=4" width="50"/> | <img src="https://avatars.githubusercontent.com/u/57372321?v=4" width="50"/> | <img src="https://avatars.githubusercontent.com/u/47994171?v=4" width="50"/> | <img src="https://avatars.githubusercontent.com/u/113833060?s=400&u=0ce27cd0e5383024e23f18fcbff0f09ec3688896&v=4" width="50"/> | <img src="https://avatars.githubusercontent.com/u/87736238?s=400&u=298a7edde9918b0a5900afb1d27c7e1b5343555f&v=4" width="50"/> | <img src="https://avatars.githubusercontent.com/u/176295257?v=4" width="50"/> |
|:--:|:--:|:--:|:--:|:--:|:--:|
| [**Qiyue Zhang**](https://github.com/yuk1Zhang) | [**Peng Cheng**](https://github.com/jackie174) | [**longxin Li**](https://github.com/heizi1307) | [**Haoyuan Qin**](https://github.com/QHYY2002) | [**Huacong Ying**](https://github.com/eareyemouthheart) |[**Shiwen Ye**](https://github.com/xx) |
