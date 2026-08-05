# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sigma Touch is an Android app that ports PC games to touch devices, built on the shared OpenTouch infrastructure. It was scaffolded from Psi Touch and currently ships three engines — **UE1** (Unreal), **UT99** (Unreal Tournament) and **AVP** (Aliens vs Predator); see [Engines](#engines) below for each one's status and notes. The app itself is a thin host: nearly all UI, controls, storage, and SDL plumbing live in the `AndroidCore_OpenTouch` library submodule.

- **App module namespace:** `com.opentouchgaming.sigmatouch`
- **Min SDK:** 19 (Android 4.4) · **Target/Compile SDK:** 35
- **Languages:** Java + Kotlin (app), C/C++ (native libs)
- **NDK:** 24.0.8215888 · ABIs: `armeabi-v7a`, `arm64-v8a`

## Other Example apps
There are other apps located here, Delta Touch and Quad Touch. These have good examples of other features and ways of doing things: /Users/emilebelanger/Android/AndroidStudioProjects/OpenTouch. Psi Touch (a sibling of this repo) is the closest reference and shows how TheForceEngine and OpenJK are wired in.

## Repository Layout

This Git repo is a multi-submodule super-project. The Gradle project root is `SigmaTouch/` (not the repo root).

- `SigmaTouch/` — Gradle root (`settings.gradle`, `build.gradle`, `gradlew`). Run all Gradle commands from here.
- `SigmaTouch/sigmatouch/` — the app module (`:sigmatouch`).
- `AndroidCore_OpenTouch/` — submodule providing the `:androidcore` and `:changeloglib` library modules. **Most shared behavior lives here; see its own `CLAUDE.md`.**
- `SAFFAL/` — submodule providing `:saffal` (scoped-storage file abstraction).
- `SigmaTouch/sigmatouch/src/main/jni/` — native sources, mostly submodules (`SDL2_OpenTouch`, `Clibs_OpenTouch`, `MobileTouchControls`, `gl4es`, `AudioLibs_OpenTouch`).
- `SigmaTouch/sigmatouch/src/main/jni/Games/` — each game engine wrapped as its own Gradle library module, registered in `settings.gradle`. Currently: `Games/Unreal/` (`:Unreal`, engine source at `Games/Unreal/src/main/jni/UE1`), `Games/UT99/` (`:UT99`, engine source at `Games/UT99/src/main/jni/ut99dc`) and `Games/AVP/` (`:AVP`, engine source at `Games/AVP/src/main/jni/NakedAVP`).

After cloning, initialize submodules: `git submodule update --init --recursive`.

## Build Commands

Run from `SigmaTouch/`:

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew installDebug         # Build + install on connected device
./gradlew clean
./gradlew :Unreal:assembleDebug -PabiOverride=arm64-v8a   # One engine, one ABI (fast iteration)
```

Command-line builds need a JDK ≤ 21 — the system default may be newer and breaks Gradle 8.13 ("Unsupported class file major version"). Use Android Studio's JBR: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew ...`

### Native build model

`src/main/jni/Android.mk` builds SDL2, touchcontrols, gl4es, audio libs, SAFFAL, etc. into `src/main/libs/<abi>/*.so` via `ndk-build`. The app module's own `externalNativeBuild` points at `Android_dummy.mk` (empty) on purpose, so Gradle does not rebuild these automatically — rebuild them with `ndk-build` against `Android.mk` when the underlying submodules change. Engine modules (`:Unreal`) build through CMake and link against these prebuilt `.so`s.

## Architecture

### Host ↔ Core split

The app provides only app-specific configuration and engine wiring; the heavy lifting is in `:androidcore`.

- **`EntryActivity`** (`:sigmatouch`) — launcher activity. Its static initializer registers `AppInfo.gameEngines` (currently just UNREAL), tutorials, scoped-storage tutorial, storage examples, and user-file entries. Calls `AppInfo.setAppInfo(... Apps.SIGMA_TOUCH ...)` in `onCreate`. **To add an engine, add a `GameEngine` entry to the `gameEngines` array here.**
- **`SigmaFragment extends MainFragment`** (`:androidcore`) — owns per-engine `GameLauncherInterface` instances (`UE1Launcher`) and the `launchGame()` flow. `setLauncher()` switches the active launcher based on `AppInfo.currentEngine.engine`.

### Adding an engine

Each engine needs three pieces wired together (see Psi Touch for concrete examples):

1. A `GameLauncherInterface` impl (e.g. `TFELauncher.kt`) registered in `SigmaFragment`.
2. An `EngineOptionsInterface` impl under `engineoptions/` — settings dialog + `RunInfo`, referenced from the `GameEngine` entry in `EntryActivity`.
3. A native engine module under `jni/Games/` registered in `SigmaTouch/settings.gradle` and depended on in `sigmatouch/build.gradle`.

`SIGMA_TOUCH` is registered in `AppInfo.Apps` and shares Psi's gamepad definition in `GamepadDefinitions` (both in `:androidcore`).

### Engines

Each engine's detailed notes — every Android-specific patch, the reasoning behind it, and the traps hit along the way — live in their own file under [`docs/engines/`](docs/engines/). **Read the relevant one before touching that engine**; they are where the hard-won detail is.

| Engine | Module | Run dir | Status | Notes |
|---|---|---|---|---|
| Unreal (UE1 v200) | `:Unreal` | `/OpenTouch/Sigma/UE1` | Verified running on-device — menu, gameplay, audio, resolution, HUD scale all working | [`docs/engines/ue1.md`](docs/engines/ue1.md) |
| Unreal Tournament (UE1 v400) | `:UT99` | `/OpenTouch/Sigma/UT99` | Phase 2 well advanced — boots, mouse-navigable menu, audio, gameplay input implemented | [`docs/engines/ut99.md`](docs/engines/ut99.md) |
| Aliens vs Predator (NakedAVP) | `:AVP` | `/OpenTouch/Sigma/AVP` | Phase 2 — runs on-device (primary storage), menus and gameplay input working, Smacker plot FMVs and Bink intros/outros/menu-backdrop/music implemented | [`docs/engines/avp.md`](docs/engines/avp.md) |

Key facts that affect work outside any single engine:

- **`:Unreal` and `:UT99` are deliberately separate engines.** They are different UE1 generations (package versions 61–67 vs 68) and their content is mutually incompatible, so neither can load the other's data.
- **`:AVP` is the only SDL3 engine**; everything else is SDL2. `RunInfo.sdlVersion` picks the SDL activity (`org.libsdl.app3000` vs `app2012`) in `SigmaFragment.launchGame()`, and `jni/Android.mk` sets `SDL3_ENABLED=1` to build `libSDL3.so` alongside SDL2.
- **`:AVP` is the only engine using gl4es**; the two UE1-family engines have real GLES renderers of their own.
- **`:AVP` is the only engine linking the shared FFmpeg prebuilts** (`Clibs_OpenTouch/ffmpeg`, static + PIC), for Bink video/audio. Those prebuilts are shared with q2repro in Delta/Quad Touch, so rebuilding them affects those apps too — see [`docs/engines/avp.md`](docs/engines/avp.md) for the recipe.
- All three engine checkouts are submodules of forks under `emileb/` (`UE1`, `ut99dc`, `NakedAVP`); AVP's is the SSH remote `git@github.com:emileb/NakedAVP.git` and sits on the `mobile_main` branch.

### Storage

Uses the OpenTouch storage model: legacy path `/OpenTouch/Sigma/` plus Android R+ scoped storage.

## Conventions & Gotchas

- **Comments: keep them simple and concise.** Prefer a single short line explaining the *why*.
- **ViewBinding is enabled** (`viewBinding true`).
- **Glide is pinned to 4.16.0** and `constraintlayout` to `1.1.3` / `recyclerview` to `1.3.2` for Nvidia Shield TV + API 19 compatibility — do not upgrade without testing on those targets.
- Native `.so` outputs in `src/main/libs/` are committed-elsewhere build artifacts; regenerate via `ndk-build` if a native submodule changes.
- The Gradle configuration cache is enabled.

## Porting a new game engine

These 'Touch' apps support multiple game engines. The full guide has been split out of this file:

- [`docs/porting/phase1.md`](docs/porting/phase1.md) — Phase 1: get a new engine building and booting to its menu.
- [`docs/porting/phase2.md`](docs/porting/phase2.md) — Phase 2: make it playable (real input, touch controls, user-file redirection, audio, resolution).
- [`docs/porting/gotchas.md`](docs/porting/gotchas.md) — cross-engine lessons learned (shader precision, SAF paths, threading, ...).

UE1 is the concrete example behind nearly every item in those guides — see [`docs/engines/ue1.md`](docs/engines/ue1.md) for how each step played out in practice.
