# Sonara for Android (v2.6)

Local music player built with Kotlin, Jetpack Compose and Media3.

## Features
- Background playback with notification, lock-screen and headset controls
- Finds all music on the phone automatically (no importing needed)
- Every folder is its own playlist and plays on its own
- Glass-style panels over a gradient, your own photo or the album art, with blur, dimming and transparency controls
- Fully customizable visualizer (bars, color, sensitivity, height, width, shape, mirror)
- Spinning disc on the playing song
- Library saved between launches, with embedded cover art
- Search, favorites, sorting; shuffle, repeat, playback speed
- Sleep timer (15–90 min or end of song) with gentle fade-out
- Equalizer presets, bass boost, fade between songs
- Synced (.lrc) or plain lyrics, loaded per song
- Audio-reactive visualizer (optional, uses the audio-analysis permission; never records)
- Folder scanning: pick a folder and Sonara adds every song inside, subfolders included
- Playlists: create, rename, delete, add/remove songs
- Home-screen widget with cover art and previous / play / next
- Separate Customize window: pinned live preview, settings in sub-windows, manual photo cropping; changes apply only when you tap Apply
- 6 soft themes (3 light, 3 dark) with smooth color transitions; glow and shadows that follow the song's artwork; 4 visualizer styles

## Build with GitHub
Push this folder to a repo. The Actions workflow builds `app-debug.apk`
(Actions tab → run → Artifacts). Tag `v1.0` to publish it on Releases.

### Signed release APK (optional)
1. `keytool -genkeypair -v -keystore sonara.jks -alias sonara -keyalg RSA -keysize 2048 -validity 10000`
2. `base64 -w0 sonara.jks` and add repo secrets:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
3. Keep the .jks file safe; never commit it.

## Notes
- With audio-reactive off, the visualizer is a looping animation.
- Equalizer and bass boost depend on the phone's audio hardware.
