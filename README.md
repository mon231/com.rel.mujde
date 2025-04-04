# Mujde app
Mujde is an android app in form of Xposed module, that integrates with Xposed framework to hook apps. <br />
Mujde lets the user manage repository (local and remote) of frida-js scripts, and select what script should it inject to which selected application. <br />
Using android's SharedPreferences, mujde stores a map between apps to the list of scripts it should inject into them. <br />
Using Xposed's XSharedPreferences, mujde's hook (`InjectionRequester` cls) is accessible to that mapping.

Via Xposed framework, mujde gets a callback (`handleLoadPackage`) whenever an app is loaded by zygote-forkAndSpeciallize. <br />
Then, mujde fetches the list of scripts it should inject to that app (using `XSharedPreferences`). <br />
If that list isn't empty, mujde (`installHookOnActivityCreation`) installs hook on `Activity.onCreate` function, <br />
Which lets us get a callback whenever a new activity is started in that app (which we already know that has scripts the user wants to inject into). <br />
Then, from that hook, mujde sends an injection-request from the app's context to mujde's broadcast-listener named `InjectionRequestHandler`. <br />
That handler sends the injection-request to mujde's injection service (`FridaInjectorService`) via intent, <br />
And the service creates `frida-inject` subprocesses (as root user) to inject the selected scripts into the started app. <br />
NOTE that the scripts will be re-injected whenever the app creates/re-creates any activity.

Mujde manages the local scripts repository in the app-data folder's `files` subfolder. <br />
Users with root permissions can create/remove/edit files from that folder directly, <br />
Without using mujde's app to manage the repository: `/data/data/com.rel.mujde/files/scripts` <br />
This feature is extremly usable for scripts / massive management operations on the repository.

Additional, mujde's shared-preferences are stored at Xposed-shared-preferences folder to be world-readable. <br />
Root users can edit the shared-preferences from there to import settings from other devices, <br />
And any user can read the contents of that folder and files. Mujde's main shared-pref can be found at `/data/misc/eb08c078-3e01-43a8-8857-4e57e0f0e2e3/prefs/com.rel.mujde/mujde_prefs.xml`. <br />
NOTE that this path might have a different guid on different android devices / between reboots.

## Compile The App
Use this guide to compile the app by yourself (or simply download the build artifacts from github-action or releases) <br />
NOTE you can compile the app using windows / linux / macos

### Requirements
- git client
- java setup (e.g. java-temurin v21) and android-sdk
- java-home (env. variable ANDROID_HOME) or local.properties set correctly with `sdk.dir`
- internet connection (in order to download `frida-inject` binaries from frida's github releases)
- python3 + pip + `pip install signapp buildapp && buildapp_fetch_tools`
    * optional, only made for release builds

### Build steps
Clone the project:
```bash
git clone https://github.com/mon231/com.rel.mujde mujde
```

Execute gradlew:
```bash
cd mujde/client
./gradlew assembleDebug
```

Find the apk at `mujde/client/app/build/outputs/apk/debug/app-debug.apk`
In order to build the app for release compilations:
```bash
./gradlew assembleRelease
signapp -a mujde/client/app/build/outputs/apk/release/app-release.apk -o app-signed.apk
```

Find the apk (release build) at `app-signed.apk`

## Install The App
The app is implemented in form of Xposed module, therefore MUST be installed on devices with Xposed-framework, like LSposed/EdXposed. <br />
On devices with such framework (e.g. rooted devices with LSPosed over zygisk, or virtual-LSposed over normal android/vm), <br />
Simply install the app and enjoy hooking other apps, automatically & covertly! <br />

If the app is installed over devices with no Xposed framework, it might simply crash when opened. <br />
Use network guides to install LSposed / root your device, or use an emulator with similar guides.

## Code Classes

### Fragments
ScriptsFragment: show the scripts page from navbar. let the user watch existing scripts, create new, edit, delete, ...
HomeFragment: shows home screen from navbar, the first fragment of the main activity, let's user set remote-repository addr
AppsFragment: show list of apps. when app is selected, creates the ScriptSelectionActivity for chosen app (via intent's extra)

### Services
FridaInjectorService: a background service that waits for notifications from InjectionRequestHandler then executes frida-inject process to inject requested scripts to the app

### Xposed-classes
InjectionRequester: notified when a new injectible app is loaded, hooks it's Activity.onCreate method to broadcast a com.rel.mujde.INJECT_REQUEST request

### DATA-classes
InjectionRequest: simple data-holder to serialize/deserialize request content in Intent's extra
Constants: contains consts and strings for global needs
api.model.Script: struct to hold script info (name, id, ...)

### Handlers
InjectionRequestHandler: handles broadcasted com.rel.mujde.INJECT_REQUEST requests, notifies the FridaInjectorService
BootCompletedHandler: handles broadcasted ACTION_BOOT_COMPLETED msg, attempts to start the background service (* might fail in new android devices, as such service must be started from GUI activity)

### Utils
api.ApiClient: util to get Retrofit client for ScriptServer
api.ScriptServer: wrapper for HTTP restapi calls (for Retrofit client)
ScriptUtils: utils about scripts (repository, SharedPref, content, files, folder, ...)
AccessibilityUtils: chmod files / folders to make sure they are readable from other users

### Activities
ActivityMain: the activity which contains the fragments and the footer navbar
ScriptSelectionActivity: a screen where the user selects what scripts should be injected to currently selected app
RemoteScriptsActivity: a screen where the user views, downloads and deletes scripts from the remote server

### Adapters
AppListAdapter: used to list all installed apps and move to ScriptSelectionActivity to choose what scripts to inejct
ScriptCheckboxAdapter: used by ScriptSelectionActivity, to track availableScripts/selectedScripts
ScriptAdapter: used by ScriptsFragment to view each script-item (script name, edit button, delete button)
RemoteScriptAdapter: views item per script (script id, name, delete button, date, ...)
