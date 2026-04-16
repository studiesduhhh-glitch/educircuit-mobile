# EduCircuit Mobile

A fresh Android-first app inspired by the Educircuit idea, built separately from the existing Educircuit web project.

This is not a WebView wrapper and it does not copy files from the old app. It is a native Java Android app with its own circuit board, simulator, dashboard, teacher panel, project storage, and on-device coach.

## Features

- Touch circuit board with draggable components and tappable ports.
- Component palette: Battery, Resistor, LED, Switch, Motor, Buzzer, Soil Sensor, and Pump.
- Auto Wire builds a safe starter loop.
- Run Lab checks closed loops, low voltage, short-style unsafe loops, and missing LED resistors.
- Student dashboard shows status, score, grade, outputs, battery voltage, and coach feedback.
- Teacher panel supports grade, feedback, and copyable project summaries.
- AI Coach gives local rule-based help for voltage, resistors, short circuits, quizzes, and exact fixes.
- Save and Load stores the latest project on the device with SharedPreferences.

## Build

1. Install Android Studio with JDK 17 or newer.
2. Open this folder in Android Studio:

   `/Users/achyutaownsvishnu/Documents/educircuit-mobile`

3. Let Gradle sync.
4. Run the `app` configuration on an emulator or Android phone.

The project now includes a Gradle wrapper, so Android Studio can sync directly and terminal builds can use:

```sh
./gradlew assembleDebug
./gradlew lintDebug
```

The debug APK is created at:

`app/build/outputs/apk/debug/app-debug.apk`

## Notes

- Package: `com.educircuit.mobile`
- Minimum Android version: API 23.
- No internet permission is used in this first native version.
- Debugged with Android Studio's bundled JDK and a Pixel 5 emulator.
