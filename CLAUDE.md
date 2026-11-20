# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository. Guidance shared by all the OpenTouch apps (architecture, native build model, conventions, the engine porting guide) is in the parent `../CLAUDE.md`, which loads automatically.

## Project Overview

Sigma Touch is an Android app that ports PC games to touch devices, built on the shared OpenTouch infrastructure. It was scaffolded from Psi Touch and currently ships three engines — **UE1** (Unreal), **UT99** (Unreal Tournament) and **AVP** (Aliens vs Predator) — with **Quake 4**, **Far Cry** and **UT2004** in progress behind build switches; see [Engines](#engines) below for each one's status and notes. The app itself is a thin host: nearly all UI, controls, storage, and SDL plumbing live in the `AndroidCore_OpenTouch` library submodule.

- **App module namespace:** `com.opentouchgaming.sigmatouch`
- **Min SDK:** 19 (Android 4.4) · **Target/Compile SDK:** 35
- **Languages:** Java + Kotlin (app), C/C++ (native libs)
- **NDK:** 24.0.8215888 · ABIs: `armeabi-v7a`, `arm64-v8a`

Psi Touch (`../Psi_Touch`) is the closest sibling reference.

## Repository Layout

This Git repo is a multi-submodule super-project. The Gradle project root is `SigmaTouch/` (not the repo root).

- `SigmaTouch/` — Gradle root (`settings.gradle`, `build.gradle`, `gradlew`). Run all Gradle commands from here.
- `SigmaTouch/sigmatouch/` — the app module (`:sigmatouch`).
- `AndroidCore_OpenTouch/` — submodule providing the `:androidcore` and `:changeloglib` library modules. **Most shared behavior lives here; see its own `CLAUDE.md`.**
- `SAFFAL/` — submodule providing `:saffal` (scoped-storage file abstraction).
- `SigmaTouch/sigmatouch/src/main/jni/` — native sources, mostly submodules (`SDL2_OpenTouch`, `Clibs_OpenTouch`, `MobileTouchControls`, `gl4es`, `AudioLibs_OpenTouch`).
- `SigmaTouch/sigmatouch/src/main/jni/Games/` — each game engine wrapped as its own Gradle library module, registered in `settings.gradle`. Currently: `Games/Unreal/` (`:Unreal`, engine source at `Games/Unreal/src/main/jni/UE1`), `Games/UT99/` (`:UT99`, engine source at `Games/UT99/src/main/jni/ut99dc`), `Games/AVP/` (`:AVP`, engine source at `Games/AVP/src/main/jni/NakedAVP`), `Games/Quake4/` (`:Quake4`, `openQ4` + `openQ4-game`), `Games/FarCry/` (`:FarCry`, engine source at `Games/FarCry/src/main/jni/NearChuckle`) and `Games/UT2004/` (`:UT2004`, `engine-ut2004` + its `gl4es-ut2004` and `metoolkit-karma-decomp`).

After cloning, initialize submodules: `git submodule update --init --recursive`.

## Build Commands

Run from `SigmaTouch/`:

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew installDebug         # Build + install on connected device
./gradlew clean
./gradlew :Unreal:assembleDebug -PabiOverride=arm64-v8a   # One engine, one ABI (fast iteration)
./gradlew assembleRelease -PincludeQuake4=false            # Ship without the unfinished Quake 4
```

### Leaving an engine out of a release: `includeQuake4`

`includeQuake4` (declared in `SigmaTouch/gradle.properties`, default `true`) is the switch for shipping while Quake 4 is unfinished. Set it to `false` there, or pass `-PincludeQuake4=false`, and the APK loses the `:Quake4` dependency (so `libquake4.so` and the two modules it `dlopen()`s are never built or packaged), the staged `baseoq4` assets, and Quake 4's entries in the engine list and the user-files dialog.

`includeFarCry` and `includeUT2004` do the same for `:FarCry` and `:UT2004`. Four pieces make that work, and any future engine needing the same treatment should copy them:

- `settings.gradle` only `include`s `:Quake4` when the property is on, so a release never configures its CMake at all.
- `sigmatouch/build.gradle` reads the property once, guards `implementation(project(":Quake4"))` with it, and exposes it as `BuildConfig.INCLUDE_QUAKE4`.
- `EntryActivity` appends the `GameEngine` entry and the `UserFileEntryDescription` inside `if (BuildConfig.INCLUDE_QUAKE4)`. Everything else in the app module (`Quake4Launcher`, `EngineOptionsQuake4`, the drawables) compiles and ships either way — harmless, and it keeps the switch to one guard per list.
- The engine's staged pk4/`mod.json` assets live in **`Games/Quake4/src/main/assets/quake4/`**, not the app's own assets. AGP merges a library module's assets into the APK, so they leave with the dependency instead of needing a separate exclusion.

Verify a flip with `mergeDebugAssets` / `mergeDebugNativeLibs` and look at `sigmatouch/build/intermediates/` rather than unpacking an APK.

### Native build model

Follows the shared model in `../CLAUDE.md`. The prebuilt `.so`s in `src/main/libs/` were built with NDK 27.3, not the Gradle NDK 24 (mixing NDK 24 into its leftover `obj/` objects fails to link libc++).

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
| Quake 4 (openQ4) | `:Quake4` (0.13.2), `:Quake4Dev` ('dev', openQ4 0.14.1) | `/OpenTouch/Sigma/Quake4` | Renders in-game 3D on-device through the ES 3.0 backend, pointer-driven menus, touch overlay working; gameplay input goes in by usercmd action, lightly played. Two versions in one APK, picked with the version button; dev also plays The Awakening (alpha) and runs on device. **Behind the `includeQuake4` build switch** (covers both modules) — see [Build Commands](#build-commands) | [`docs/engines/quake4.md`](docs/engines/quake4.md) |
| Far Cry (NearChuckle) | `:FarCry` | `/OpenTouch/Sigma/FarCry` | Phase 2 done — plays by touch on-device through its own ES 3.0 renderer (sticks, swipe look, actions by name, menu/game overlay switching, user files under `user_files/farcry/`); played through Training and the start of Pier (machine gun, sniper, checkpoint reload). Behind the `includeFarCry` build switch (same shape as `includeQuake4`) | [`docs/engines/farcry.md`](docs/engines/farcry.md) |
| Unreal Tournament 2004 (engine-ut2004) | `:UT2004` | `/OpenTouch/Sigma/UT2004` | Phase 1 wired, not yet run on a device — `libut2004.so` builds for both ABIs with Sigma's SDL2, touch controls, SAFFAL and openal-soft; basic touch input. Behind the `includeUT2004` build switch | [`docs/engines/ut2004.md`](docs/engines/ut2004.md) |

Key facts that affect work outside any single engine:

- **`:Unreal` and `:UT99` are deliberately separate engines.** They are different UE1 generations (package versions 61–67 vs 68) and their content is mutually incompatible, so neither can load the other's data.
- **`:AVP`, `:Quake4` and `:FarCry` are the SDL3 engines**; the UE1-family ones and `:UT2004` are SDL2. `RunInfo.sdlVersion` picks the SDL activity (`org.libsdl.app3000` vs `app2012`) in `SigmaFragment.launchGame()`, and `jni/Android.mk` sets `SDL3_ENABLED=1` to build `libSDL3.so` alongside SDL2.
- **`:Quake4` is the arm64-only engine; it and `:FarCry` are the ones that ship more than one `.so`.** Far Cry builds both ABIs since 2026-10-01 (32-bit arm uses the engine's 64-bit Linux configuration; see [`docs/engines/farcry.md`](docs/engines/farcry.md)). Quake 4 builds `libquake4.so` plus two modules the engine `dlopen()`s at runtime (`librenderer-gles_arm64.so`, `libgame-sp_arm64.so`), mirroring openQ4's own desktop layout; Far Cry builds `libfarcry.so` plus fifteen `libCry*.so`/`libXRenderGLES.so` modules loaded by name through `MODULE_PATH` (see [`docs/engines/farcry.md`](docs/engines/farcry.md)). It is also the only engine whose source of truth is Meson — its Android `CMakeLists.txt` reuses openQ4's own source-list scripts so the two builds cannot drift. See [`docs/engines/quake4.md`](docs/engines/quake4.md).
- **`:AVP` uses the app's shared `libGL4ES.so`; `:UT2004` links its own gl4es fork statically** (the touch overlay then draws with plain GLES and restores gl4es's state, see [`docs/engines/ut2004.md`](docs/engines/ut2004.md)). The two UE1-family engines have real GLES renderers of their own.
- **`:AVP` is the only engine linking the shared FFmpeg prebuilts** (`Clibs_OpenTouch/ffmpeg`, static + PIC), for Bink video/audio. Those prebuilts are shared with q2repro in Delta/Quad Touch, so rebuilding them affects those apps too — see [`docs/engines/avp.md`](docs/engines/avp.md) for the recipe.
- Engine checkouts are submodules of forks under `emileb/` (`UE1`, `ut99dc`, `NakedAVP`, `openQ4`, `NearChuckle`); AVP's, openQ4's and NearChuckle's are SSH remotes. openQ4's GLES/macOS work sits on its **`main_gles`** branch, and the submodule tracks **`main_gles_android`**, which is the Android commits rebased on top of `main_gles`. Its companion game-library repo `openQ4-game` is a second submodule. The 'dev' version's own `openQ4` submodule (under `Games/Quake4Dev/`) tracks **`dev_gles_android`** (0.14.1, game code in-tree, no `openQ4-game`). See [`docs/engines/quake4.md`](docs/engines/quake4.md). `:UT2004`'s three checkouts are not submodules yet (no `emileb/` forks); its engine works on an `android` branch on top of `macos-port`, as NearChuckle does.

### Storage

Uses the OpenTouch storage model: legacy path `/OpenTouch/Sigma/` plus Android R+ scoped storage.

## Conventions & Gotchas

Shared conventions (comments, ViewBinding, pinned dependencies, `nonFinalResIds`) are in `../CLAUDE.md`.

## Porting a new game engine

The generic guide is shared by all apps in [`../docs/porting/`](../docs/porting/). UE1 is the running example behind it — see [`docs/engines/ue1.md`](docs/engines/ue1.md) for how each step played out in practice.
