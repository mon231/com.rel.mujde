# Languager - Language Spoofing Xposed Module

## Overview
Languager is an Xposed module that allows you to change the language of specific Android applications, without changing your system language. This is particularly useful against apps that enforce system language settings or buggy apps that fail to show the selected language from app's settings.

## Features
- **Per-App Language Selection**: Choose a different language for each app independently
- **Comprehensive Language Support**: 26+ languages available including English, French, German, Spanish, Italian, Portuguese, Russian, Chinese, Japanese, Korean, Arabic, Hebrew, Hindi, and many more
- **User-Friendly Interface**: Easy-to-use UI with app search functionality and intuitive language selection
- **Wide Compatibility**: Works on Android 5.0+ (API 21+) with support for both older and newer Android versions
- **Efficient Hooking Mechanism**: Hooks all locale-related methods to ensure complete language spoofing

## Technical Details
- **Language Spoofing Mechanism**:
  - Uses Xposed framework for comprehensive method hooking
  - Targets all locale-related methods across different API levels:
    - `Configuration.getLocales()` (API 24+)
    - `Configuration.getLocale()` (API < 24)
    - `Configuration.locale` property
    - `Resources.getConfiguration().locale`
    - `Locale.getDefault()`
    - `Locale.getDefault(Category)` (API 24+)
    - `LocaleList.getDefault()` (API 24+)
    - `LocaleList.getAdjustedDefault()` (API 24+)
  - Dynamically replaces system locale with user-selected language
  - Handles API differences between Android versions

- **Shared Preferences**:
  - Uses XSharedPreferences for cross-process configuration storage
  - Implements world-readable preferences for Xposed module access
  - Direct key-value storage for app-language mappings

## Requirements
- Android device with Xposed framework installed (LSPosed, EdXposed, etc.)
- Android 5.0+ (API 21+)

## Installation
1. Install the Languager APK
2. Enable the module in your Xposed framework manager
3. Select the apps you want to apply language spoofing to
4. Reboot your device
5. Open the Languager app to configure language settings for each app

### LSPosed Repository Installation
You can also install Languager directly from the LSPosed repository:
1. Open LSPosed Manager
2. Go to Repository tab
3. Add the repository URL: `https://mon231.github.io/com.rel.languager`
4. Find Languager in the list and install it
5. Enable the module and reboot

## Usage
1. Open the Languager app
2. Use the search bar to find specific apps
3. Select your preferred language for each app from the dropdown menu
4. Click "Save Settings" to apply changes
5. Reboot or restart the target apps for changes to take effect

## Supported Languages
- English
- French (Français)
- German (Deutsch)
- Spanish (Español)
- Italian (Italiano)
- Portuguese (Português)
- Russian (Русский)
- Chinese (中文)
- Japanese (日本語)
- Korean (한국어)
- Arabic (العربية)
- Hebrew (עברית)
- Hindi (हिन्दी)
- Bengali (বাংলা)
- Punjabi (ਪੰਜਾਬੀ)
- Tamil (தமிழ்)
- Telugu (తెలుగు)
- Malayalam (മലയാളം)
- Thai (ไทย)
- Vietnamese (Tiếng Việt)
- Indonesian (Bahasa Indonesia)
- Malay (Bahasa Melayu)
- Turkish (Türkçe)
- Dutch (Nederlands)
- Polish (Polski)
- Swedish (Svenska)

## Implementation Details
- **Preference Handling**: Uses `MODE_WORLD_READABLE` with LSPosed's `xposedsharedprefs` flag for secure preference sharing
- **Dynamic Locale Creation**: Creates locale objects for all supported languages
- **API Level Detection**: Automatically detects device API level and uses appropriate hooking methods
- **UI Optimization**: Efficient app list loading with search functionality
- **Compatibility Layers**: Special handling for different Android versions and manufacturer customizations

## Building from Source

### Prerequisites
- Android Studio 4.0+
- JDK 11+
- Gradle 7.0+

### Local Build (debug-mode)
1. Clone the repository:
   ```
   git clone https://github.com/mon231/com.rel.languager languager
   cd languager
   ```

1. Build:
   ```
   ./gradlew clean assembleDebug
   ```

1. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`

## Troubleshooting
- **Module not working**: Make sure it's enabled in LSPosed/EdXposed Manager
- **Language not changing**: Try restarting the app or rebooting your device

## Acknowledgements
- [Xposed Framework](https://github.com/rovo89/XposedBridge)
- [LSPosed Framework](https://github.com/LSPosed/LSPosed)
