# Syncing Nexus Music with upstream SimpMusic

```bash
scripts/sync-upstream.sh                 # merge upstream dev into nexus/main (both repos)
scripts/sync-upstream.sh --continue      # after resolving what it stopped on
scripts/sync-upstream.sh --branch nexus/sync-test   # rehearse on a throwaway branch first
```

Upstream is `https://github.com/maxrave-dev/simpmusic` (remote `upstream`, branch `dev`). The
`core/` submodule follows `https://github.com/maxrave-dev/core` (its `origin`). Nothing is ever
pushed by the script.

## What the script does, and why core is special

1. Fetches `upstream/dev` and core's `origin`.
2. **core/**: merges the commit upstream pins (`git ls-tree upstream/dev core`) *into core's own
   `nexus/main`*. It never checks out upstream's pin directly: core carries Nexus commits (updater
   removal, local playback, bit-perfect audio), and moving the gitlink to the bare upstream pin
   would silently drop all of them.
3. **App repo**: `git merge --no-commit upstream/dev`, then `git add core` to point the gitlink at
   the merged core commit. Every sync has a gitlink conflict (both sides moved core); this is
   what resolves it.
4. Keeps deleted the files Nexus deleted on purpose (list in the script: `NEXUS_DELETED`,
   `CORE_NEXUS_DELETED`) when upstream modified them.
5. **Guards**, which stop the commit:
   - updater code is back (identifiers like `checkForUpdate`, `UpdateRepository`, `GithubResponse`,
     `FdroidResponse`, `checkOfficialBuild`, `autoCheckForUpdates`, `releases/latest` in code,
     comments ignored);
   - a workflow not named `nexus-*.yml` exists in `.github/workflows/`;
   - `applicationId`, `rootProject.name` or the Nexus version wiring changed.
6. Commits `Merge upstream SimpMusic dev (<sha>) into nexus/main`.

Exit codes: `3` core conflict · `4` app-repo conflict · `5` guard hit. In each case the merge is
left in progress; resolve, `git add`, re-run with `--continue`. To back out: `git merge --abort`
in the app repo, and in core `git merge --abort` (if still merging) or `git reset --hard ORIG_HEAD`
(if the core merge was already committed by the script).

After any sync, build before trusting it:
`./gradlew -PisFullBuild=false :androidApp:assembleDebug :composeApp:compileKotlinJvm`.

## Where conflicts will appear, and how to resolve them

Rule of thumb: **take upstream's change, then re-apply the Nexus intent** described below. The
per-phase details (and the reasons) are in CLAUDE.md, section "Nexus Music fork".

### 1. Branding (Phase 1) — small, mechanical

| File | Nexus change | Resolution |
|---|---|---|
| `androidApp/build.gradle.kts`, `vivoAndroidApp/build.gradle.kts` | `applicationId = "org.nexusmusic.app"`; versionCode/Name from `rootProject.extra` | Keep both Nexus lines; take everything else from upstream. |
| `settings.gradle.kts` | `rootProject.name = "NexusMusic"` | Keep ours. |
| `composeApp/build.gradle.kts` | `packageOfResClass` pinned; BuildKonfig version from nexus.properties + `upstreamVersionName` | Keep those lines; take the rest (new deps, tasks) from upstream. |
| `androidApp/src/main/AndroidManifest.xml`, `res/xml/shortcuts.xml`, `src/debug/res/xml/shortcuts.xml` | `${applicationId}` authorities, Nexus package in shortcuts | Keep ours; if upstream adds a shortcut, add it to **both** shortcuts files with the Nexus package (`.dev` in the debug copy). |
| `**/values/app_name.xml` | "Nexus Music" | Keep ours. |
| `mipmap-*/ic_launcher*.webp`, `drawable-v24/ic_launcher_foreground.xml` | deleted (vector icons instead) | Keep deleted (automatic). |
| `conveyor.conf`, `desktopApp/build.gradle.kts`, `DesktopApp.kt`, `Main.kt`, `WindowsProtocolRegistrar.kt`, `CrashDialog.kt`, `MiniPlayerWindow.kt` | Nexus names / app id on Desktop | Keep the Nexus names; take upstream's logic. |
| `MainActivity.kt` | `checkOfficialBuild` not called | Keep it not called (only accepts SimpMusic's signing key). |
| strings mentioning "SimpMusic" in `values/strings.xml` | some read "Nexus Music" | New upstream strings may say SimpMusic; rename user-facing ones if you care. Other locales are upstream's. |

### 2. Removed in-app updater (Phase 2) — **the one to watch**

Removed: startup update checks (`MainActivity`, `DesktopApp.kt`), the update dialog in `App.kt`,
three About rows in `SettingScreen.kt`, `SharedViewModel.checkForUpdate`/`shouldCheckForUpdate`/
`updateResponse`/`isCheckingUpdate`, the update state in `SettingsViewModel`, and in core
`UpdateRepository` + impl + `UpdateData` + Koin binding, the `autoCheckForUpdates`/`updateChannel`
DataStore keys, `GITHUB`/`FDROID`/`GITHUB_FOSS_NIGHTLY` constants, and the scraper's GitHub
`releases/latest` / F-Droid calls with `GithubResponse`/`FdroidResponse`.

**Re-added updater code.** Upstream keeps working on its updater, so a sync can bring it back in
three ways:

- *Modify/delete conflict* on a deleted file (e.g. `UpdateRepositoryImpl.kt`): the script keeps
  it deleted. Then check whether upstream's change also touched callers.
- *Content conflict* in a file Nexus edited (`App.kt`, `SharedViewModel.kt`, `SettingScreen.kt`,
  `SettingsViewModel.kt`, `MainActivity.kt`, `DesktopApp.kt`, core `DataStoreManager*`,
  `RepositoryModule.kt`, `YouTube.kt`, `Ytmusic.kt`): take upstream's side **minus** any updater
  call, state, dialog or settings row.
- *Clean merge that silently re-adds it*: a new file or a new call site in a file Nexus never
  touched. Git reports no conflict; **the guard is what catches this** (exit 5). Delete the new
  code, or if it calls a removed symbol, remove the call. Never restore `UpdateRepository` just to
  make a new caller compile.

Also expect Kotlin compile errors after a clean merge if upstream added a new reference to a
removed symbol in a file the guard pattern doesn't name; the post-sync build catches those.

### 3. Liquid Glass (Phases 3a–3c)

New files (`ui/component/glass/*`, `GlassLabScreen.kt`, `GlassLabDestination.kt`,
`NowPlayingContentLiquidGlass.kt`, `glass/GlassPlatform.*.kt`) never conflict. Conflicts come from
call-site edits in ~40 screens: top bars (`glassBarBlur(...)` instead of `hazeBlur(... barBlurStyle ...)`),
popups (`GlassModalBottomSheet` / `GlassAlertDialog` / `GlassDropdownMenu` instead of M3's), tiles
(`glassTile()`), `Theme.kt`, `SurfaceDarkColors.kt`, `NowPlayingScreen.kt` (style registration).
Resolution: take upstream's version of the screen, then re-apply the swap: **every** M3
`ModalBottomSheet(` / `AlertDialog(` / `DropdownMenu(` in composeApp goes through the Glass wrapper.
A quick check after a sync: `git grep -nE "\b(ModalBottomSheet|AlertDialog|DropdownMenu)\(" composeApp/src | grep -v glass/`.
If upstream adds a new Now Playing style, the picker in Settings and `NowPlayingScreenContent`'s
`when` need the Liquid Glass branch kept alongside it.

### 4. Local playback (4a) and bit-perfect audio (4b)

Mostly new files (no conflicts). Edited upstream files, all in core unless noted:
`MediaServiceHandlerImpl.kt`, `JvmMediaPlayerHandlerImpl.kt`, `CrossfadeExoPlayerAdapter.kt`,
`MpvPlayer.kt`, `MpvPlayerAdapter.kt`, `MergingMediaSourceFactory.kt`, `AudioOutputRouter.kt`,
`MediaPlayerInterface.kt`, `DataStoreManager(.kt|Impl.kt)`, `MusicDatabase.kt`, `RepositoryModule.kt`,
`Loader.kt`, `AllExt.kt`, `Config.kt` (`LibraryChipType.LOCAL_MUSIC`), DI modules; composeApp:
`LibraryScreen.kt`, `SearchScreen.kt`, `SettingScreen.kt`, `SharedViewModel.kt`, `MiniPlayer.kt`,
`ModalBottomSheet.kt`, the four `NowPlayingContent*.kt`, `MiniPlayerRoot.kt`, `strings.xml`.

- **Room schema.** Nexus added `local_folder`/`local_track` as schema **28**. If upstream also bumps
  the database version, the two version numbers collide: renumber so Nexus's migration comes after
  upstream's, regenerate the schema JSON, and make sure an existing Nexus install migrates (never
  `fallbackToDestructiveMigration` — it wipes the library).
- **LosslessAudioSink.java** is a copy of Media3's `DefaultAudioSink`. When upstream bumps
  `media3` in `gradle/libs.versions.toml`, re-diff it against the new `DefaultAudioSink` (its header
  lists the intended differences) — it compiles against package-private Media3 classes.
- In the player adapters, keep each Nexus guard (`LocalMedia.isLocal`, `bypassActive`,
  `localCrossfadeBlocked`, `syncLocalGapless`) on **both** crossfade trigger paths, as CLAUDE.md
  already demands for every crossfade guard.

### 5. Release setup (Phase 5)

`nexus.properties`, `NOTICE`, `SYNC.md`, `scripts/build-release.sh`, `scripts/sync-upstream.sh`,
`scripts/create-release-keystore.sh`, `.github/workflows/nexus-build-check.yml` are Nexus-only.
Edited: `build.gradle.kts` (version block), `README.md` (Nexus block on top — keep it on top),
`.gitignore`, the `VersionManager` promo calls (`HomeScreen.kt`, `LyricsPromoLine.kt`,
`SharedViewModel.kt` use `getUpstreamVersionName()`; keep that for any new promo call).

- **Versions**: upstream's `version-name`/`version-code` in `gradle/libs.versions.toml` are left
  alone, so their bumps merge cleanly. Nexus numbering lives only in `nexus.properties`.
- **CI**: upstream's `android.yml`, `android-release.yml`, `desktop-package.yml`, `pr-triage.yml`
  were deleted (they sign and publish with upstream's secrets and channels). Edits to them are kept
  deleted automatically; a **new** upstream workflow trips the guard — delete it, or rename it
  `nexus-*.yml` after removing every secret, upload and publish step.
- `build_and_sign_apk.sh` and `fastlane/` are upstream's and untouched; take upstream's changes.

## Where the core commits live

Nexus core commits live on core's `nexus/main`, pushed to your own `nexus-music-core` repository.
`.gitmodules` points at it with the RELATIVE url `../nexus-music-core.git`, so it resolves to
`<owner>/nexus-music-core` next to wherever this repo is hosted, whatever the owner is called.
Only the `.gitmodules` file was changed (`git config -f .gitmodules`), NOT `git submodule set-url`:
that would also rewrite core's local `origin`, which must stay upstream's `maxrave-dev/core` for the
sync script. Upstream's own `.gitmodules` carries an absolute maxrave-dev url; on a merge conflict
there, keep ours. Push core's `nexus/main` to `nexus-music-core` BEFORE pushing this repo, or the
recorded core commit will not exist on GitHub.
