<div align="center">

# Nexus Music

**A free and open-source YouTube Music client for Android, with a dark-violet design, Liquid Glass, local music and bit-perfect lossless playback.**

[![Download](https://img.shields.io/badge/Download-latest%20APK-8B5CF6?style=for-the-badge&logo=android&logoColor=white)](../../../releases/latest)

Android 8.0 or newer · No ads · No account needed · Free and open source (GPL-3.0)

</div>

---

Nexus Music is a modified version of **[SimpMusic](https://github.com/maxrave-dev/SimpMusic)** by maxrave-dev and contributors. It is not affiliated with or endorsed by SimpMusic, YouTube or Google.

## Download

Go to **[Releases](../../../releases/latest)** and download **one** of these files:

| File | Use it for |
|---|---|
| `NexusMusic-x.y.z-nexus-arm64-v8a.apk` | **Almost every phone from the last 6–7 years. Start here.** |
| `NexusMusic-x.y.z-nexus-armeabi-v7a.apk` | Old 32-bit phones |
| `NexusMusic-x.y.z-nexus-x86_64.apk` | Emulators and x86 Chromebooks |
| `NexusMusic-x.y.z-nexus.apk` | Universal: works on any phone, but twice the size |

Not sure which one? Take the **universal** APK. It always works.

## How to install

1. Download the APK on your phone.
2. Open it. Android may ask you to allow installs from your browser or file manager. Allow it, then go back.
3. Tap **Install**.
4. Open **Nexus Music**.

Nexus Music installs as its own app (`org.nexusmusic.app`), so it can sit beside SimpMusic or YouTube Music.

**Updating:** download the newer APK from Releases and install it over the old one. Your library and settings stay. There is no in-app updater, so check Releases now and then, or click **Watch → Custom → Releases** at the top of this page to get notified.

## Features

- **Nexus Violet design.** A dark-purple theme with a violet glow, a Now Playing screen with spinning artwork in a glowing ring, and a redesigned Home.
- **Liquid Glass.** Frosted glass top bars, sheets, dialogs and menus, plus a Liquid Glass Now Playing style.
- **Local music.** Play your own music files from folders you choose. They show up in a Local tab in Library and in Search.
- **Bit-perfect lossless mode.** FLAC and other lossless files can skip all app processing, use the USB bit-perfect mixer on Android 14+, and play gapless across an album. A quality badge tells you honestly when audio is resampled or processed.
- **India by default.** Home and Charts show Indian music when you are signed out. Change it in Settings → Content country.
- **No popups or promotions.** No rating, donation or promo popups or banners.
- Everything else from SimpMusic: background playback, synced lyrics, an equalizer, crossfade, offline downloads, sleep timer, SponsorBlock and more.

## Is it safe?

- Every release is signed with the same key, so Android will only let you update it with a genuine Nexus Music build.
- Each release has a `SHA256SUMS.txt` file. To check a download on a computer:
  - Windows (PowerShell): `Get-FileHash NexusMusic-1.0.0-nexus.apk`
  - macOS / Linux: `sha256sum NexusMusic-1.0.0-nexus.apk`

  The result must match the line for that file in `SHA256SUMS.txt`.
- The full source code is in this repository, so anyone can check what the app does.
- Only download Nexus Music from this page. Copies from other sites may have been changed.

## Build it yourself

You need JDK 21 and the Android SDK.

```bash
git clone --recurse-submodules https://github.com/<owner>/nexus-music.git
cd nexus-music
./gradlew -PisFullBuild=false :androidApp:assembleDebug
```

`core/` is a git submodule ([nexus-music-core](../../../../nexus-music-core)); `--recurse-submodules` fetches it. Signed release builds use `scripts/build-release.sh`, which reads the signing key from `local.properties` or environment variables (see the top of that script). How Nexus keeps up with SimpMusic is described in [SYNC.md](SYNC.md).

## Problems or ideas?

Open an **[issue](../../../issues/new/choose)**. Please include your phone model, Android version and the Nexus Music version (Settings → About).

Please do **not** report Nexus Music problems to SimpMusic. It is a separate app.

## Credits and licence

All credit for SimpMusic itself (the YouTube Music client, player, lyrics, equalizer and everything not listed in [NOTICE](NOTICE)) belongs to [maxrave-dev and the SimpMusic contributors](https://github.com/maxrave-dev/SimpMusic). If you enjoy the app, consider supporting them.

Nexus Music is released under the **GNU General Public License v3.0**, the same licence as SimpMusic. See [LICENSE](LICENSE). [NOTICE](NOTICE) lists every change Nexus makes, with dates.

YouTube and YouTube Music are trademarks of Google LLC.
