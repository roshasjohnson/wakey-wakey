# Wakey Wakey

An Android app that wakes you before your stop.

Save the places you travel to, set how far out you want the warning, and the app notifies you when you get close. Built for dozing off on a bus, train, or cab and not wanting to miss where you get down.

## How it works

The app registers a geofence, a circle of a chosen radius around each place you are watching, with Android's location services. Android watches for the crossing itself and wakes the app only when it happens, so nothing runs in the background and battery cost stays close to zero.

Location is only requested on demand, when you tap "Am I close?".

## Features

- Search for any destination worldwide
- Save as many places as you like, each with its own alert radius between 1 and 10 km
- A switch per place, so you arm only the ones you are travelling to today
- Notification on arrival naming the place you are approaching, with the app closed
- Each alert fires once, then switches itself off, so you are not re-notified while sitting at your stop
- Watched places are re-armed automatically after a reboot
- Check current distance to any saved place on demand
- Saved places persist across restarts

## Built with

Kotlin, Jetpack Compose, and Material 3. Geofencing and location come from Google Play Services. Place search uses [Photon](https://photon.komoot.io), an open geocoder built on OpenStreetMap data, so there is no API key and no billing account. Saved state is kept in DataStore Preferences as a JSON array.

Minimum Android 8.0, API 26.

## Project layout

Flat, one file per concern, no architecture pattern:

| File | Holds |
| --- | --- |
| `MainActivity.kt` | The activity and `setContent`, nothing else |
| `HomeScreen.kt` | Heading, search field, saved list and search results |
| `PlaceSheet.kt` | Per-place bottom sheet: radius slider, distance check, delete |
| `DestinationStore.kt` | The saved place list in DataStore |
| `PlaceSearch.kt` | Photon geocoding |
| `GeofenceManager.kt` | Registering and removing fences, keyed per place |
| `GeofenceReceiver.kt` | Handles a trigger: notify, then switch that place off |
| `BootReceiver.kt` | Re-registers watched places on `BOOT_COMPLETED` |
| `Permissions.kt` | Location permission checks and the on-demand fix |
| `Notifications.kt` | Channel and arrival notification |

### Battery rules

These are deliberate and worth preserving in any change:

- No continuous location updates, ever. All background detection goes through the geofencing API, which Android schedules itself.
- No foreground service, background timer, WorkManager job, or polling loop.
- The on-demand distance check uses `PRIORITY_BALANCED_POWER_ACCURACY`, so Android can answer from wifi and cell towers instead of powering up the GPS radio.
- The radius slider writes to storage when the drag ends, not on every frame.

## Running it

Clone the repo, open it in Android Studio, and run on a device. An emulator will not exercise the geofencing properly.

The app needs location permission set to "Allow all the time" for the geofence to fire while closed. Android will not grant this from a dialog, so the app sends you to system settings for it.

On phones with aggressive battery management, exempt the app from battery optimisation, or the alert may not arrive.

## Known limitations

The radius measures a straight line, not road distance. On a winding route you may still have further to travel than the radius suggests, so tune it against your own commute.

Android checks location on its own schedule to save battery, so the alert can arrive a minute or two after you actually cross the boundary. Set the radius a little wider than you think you need.

Distance is only shown after you ask for it. There is no map and no live position, by design.

Reboot re-arming depends on the phone delivering `BOOT_COMPLETED`. Some manufacturer builds, Xiaomi, Oppo and Samsung among them, withhold it from apps that are not whitelisted in their own battery settings. If alerts go quiet after a restart, look there first.

Turning a place on while you are already inside its radius fires the alert immediately and switches it back off, which is correct but can look like the switch refused to stay on.

## Attribution

Place search data © OpenStreetMap contributors, available under the [Open Database License](https://www.openstreetmap.org/copyright).
