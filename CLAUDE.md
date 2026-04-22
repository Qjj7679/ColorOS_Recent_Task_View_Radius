# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development
- **Build APK**: `./gradlew assembleDebug --no-daemon`
- **Output Path**: `app/build/outputs/apk/debug/app-debug.apk`
- **Lint**: `./gradlew lintDebug` (Standard Android lint)
- **Check Style**: The project uses standard Kotlin coding conventions.

## Project Architecture
This is an Xposed module for ColorOS, built using the [YukiHookAPI](https://github.com/HighCapable/YukiHookAPI) framework.

### Core Components
- **Hook Entry**: `com.radius.optimization.HookEntry` (Annotated with `@InjectYukiHookWithXposed`)
- **Main Hook**: `com.radius.optimization.MainHook` - Contains the logic for intercepting `Resources.getDimensionPixelSize` within the ColorOS Launcher.
- **Configuration Storage**: `com.radius.optimization.RadiusConfigProvider` - A `ContentProvider` used to persist settings.
- **UI**: `com.radius.optimization.MainActivity` - A Jetpack Compose activity for the user to adjust the radius (16dp - 130dp).
- **Constants**: `com.radius.optimization.RadiusConfig` - Defines the authority, URI, and key for settings.

### Data Flow
1. User adjusts radius in `MainActivity`.
2. `MainActivity` writes the value to `RadiusConfigProvider` via `ContentResolver`.
3. When the ColorOS Launcher requests the `recent_task_view_radius` resource, `MainHook` intercepts the call.
4. `MainHook` queries `RadiusConfigProvider` (with in-process caching) and returns the modified value.
5. The Launcher must be restarted for changes to take effect.
