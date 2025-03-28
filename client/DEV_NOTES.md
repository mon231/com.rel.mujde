## Classes

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

code-notes with `// NOTE` made to explain non-trivial behavior
