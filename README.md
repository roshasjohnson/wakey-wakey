# Wakey Wakey

An Android app that wakes you before your stop.

Save a destination, set how far out you want the warning, and the app notifies you when you get close. Built for dozing off on a bus, train, or cab and not wanting to miss where you get down.

## How it works

The app registers a geofence, a circle of a chosen radius around your destination, with Android's location services. Android watches for the crossing itself and wakes the app only when it happens, so nothing runs in the background and battery cost stays close to zero.

Location is only requested on demand, when you tap "Am I close?".

## Features

- Search for any destination worldwide
- Set the alert radius between 1 and 10 km
- Notification on arrival, with the app closed
- Check current distance to the destination on demand
- Destination persists across restarts

## Built with

Kotlin, Jetpack Compose, and Material 3. Geofencing and location come from Google Play Services. Place search uses [Photon](https://photon.komoot.io), an open geocoder built on OpenStreetMap data, so there is no API key and no billing account. Saved state is kept in DataStore Preferences.

Minimum Android 8.0, API 26.

## Running it

Clone the repo, open it in Android Studio, and run on a device. An emulator will not exercise the geofencing properly.

The app needs location permission set to "Allow all the time" for the geofence to fire while closed. Android will not grant this from a dialog, so the app sends you to system settings for it.

On phones with aggressive battery management, exempt the app from battery optimisation, or the alert may not arrive.

## Known limitations

Geofences do not survive a reboot yet, so watching has to be started again after restarting the phone.

Only one destination can be saved at a time.

The radius measures a straight line, not road distance. On a winding route you may still have further to travel than the radius suggests, so tune it against your own commute.

Android checks location on its own schedule to save battery, so the alert can arrive a minute or two after you actually cross the boundary. Set the radius a little wider than you think you need.

## Roadmap

- Multiple saved destinations
- Re-register geofences after reboot
- Redesigned interface
- Alert fires once, then switches itself off

## Attribution

Place search data © OpenStreetMap contributors, available under the [Open Database License](https://www.openstreetmap.org/copyright).
