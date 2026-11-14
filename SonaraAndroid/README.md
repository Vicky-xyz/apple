# Sonara for Android (v2)

Local music player built with Kotlin, Jetpack Compose and Media3.

## Features
- Background playback with notification, lock-screen and headset controls
- Library saved between launches, with embedded cover art
- Search, favorites, sorting; shuffle, repeat, playback speed
- Sleep timer (15–90 min or end of song) with gentle fade-out
- Equalizer presets, bass boost, fade between songs
- Synced (.lrc) or plain lyrics, loaded per song
- Themes: Dark, Light, AMOLED, System; 7 accent colors; 6 wallpapers; 4 visualizer styles

## Build with GitHub
Push this folder to a repo. The Actions workflow builds `app-debug.apk`
(Actions tab → run → Artifacts). Tag `v1.0` to publish it on Releases.

### Signed release APK (optional)
1. `keytool -genkeypair -v -keystore sonara.jks -alias sonara -keyalg RSA -keysize 2048 -validity 10000`
2. `base64 -w0 sonara.jks` and add repo secrets:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
3. Keep the .jks file safe; never commit it.

## Notes
- The visualizer is animated for looks and is not audio-reactive.
- Equalizer and bass boost depend on the phone's audio hardware.
