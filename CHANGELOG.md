# Changes

## TV 0.1.0 — 2026-11-04

- Separate YouTube+tr TV based on SmartTube 32.69, with native D-pad navigation and ARMv7/ARM64 libraries; package `org.smarttube.stable` replaces stable SmartTube.
- Add Yandex VOT to the player settings and general settings, with Russian translation, language selection, automatic translation for following videos, original audio volume and Yandex account options.
- Keep translated audio synchronized with pause, playback, seeking and speed. Cancel pending work and release the translation player when changing or leaving a video.
- Default to live voices. If Yandex refuses a translation, retry once with the other voice mode and a fresh initial request. Ordinary voices can incorrectly report “no speech” for a video whose live-voice translation is ready.
- Preserve and expose upstream credits/licenses and isolate this build from incompatible official SmartTube updates.
- Verified installation and startup on X4 Android 11 ARMv7, native remote navigation, Russian voice-over confirmed by the user, pause/resume and a 20-second seek with matching video/voice positions. Six fixed protobuf protocol checks pass. ARM64 runtime, OAuth login and uploading a new source audio track have not been verified on-device.

## 1.0.2 — 2026-11-04

- Universal APK: preserve arm64-v8a, armeabi-v7a, x86 and x86_64 libraries instead of keeping only ARM64.
- Verified correct ELF architecture for native libraries in all four ABI folders.
- Installed and launched on an X4 box with Android 11 and primary ABI armeabi-v7a.
- Include official GmsCore 0.3.13.2.250932, verified against its published SHA-256 and successfully installed on the ARMv7 box. The older GmsCore was rejected by Play Protect on this device; security settings were not changed.
- Keep the same signing key, patch configuration and SponsorBlock label fix.
- Network playback and translation on the TV box remain untested until VPN is configured. The APK retains the mobile/tablet interface.

## 1.0.1 — 2026-11-02

- Fixed clipped SponsorBlock skip and highlight labels with enlarged system fonts.
- Buttons now use content-sized height, centered text/icons and a 40 dp minimum height.
- Verified both Russian labels on Xiaomi Pad 6 with Android 14 and font scale 125%.
- Installed as an update using the existing signing key. Application settings and account storage are retained.
- Translation and other patch bytecode are unchanged; the local fix replaces one layout XML resource.

## 1.0.0 — 2026-11-02

- Initial YouTube+tr build: YouTube 21.13.164, anddea patches 4.3.0, 74 patches.
- Verified Google account access and Russian Yandex voice-over translation on Xiaomi Pad 6.
