# Mujde
[Example of Xposed native injector](https://stackoverflow.com/a/37611434)

Other (easier?) hooking method is to execute `frida-inject` in root context via android-service, <br />
That fetches the target apps (their xposed hook sets value in XSharedPref) and executes the injector (with flag '-e')


HOOK using frida server/shell, not frida-gadget. let Mujde main activity have root access.

Other example:
```java
public class Main implements IXposedHookLoadPackage  {

    // This current package.
    private static final String PACKAGE_NAME = "com.hpnotiq.Waze_enhancer";


    /**
     * Constants useful for debugging mode.
     */
    // Whether debugging mode is on or off - configurable via Settings.
    private boolean debugMode = true;


    private static final String WAZE_ENFORCEMENT_METHOD = "isEnforcementAlertsEnabledNTV";
    private static final String WAZE_ENFORCEMENT_METHOD2 = "isEnforcementPoliceEnabledNTV";


    // The class that contains the above internal Waze methods.
    private static final String WAZE_PACKAGE = "com.waze";
    private static final String WAZE_NATIVEMANAGER_CLASS_NAME = WAZE_PACKAGE+".NativeManager";
    private static final String WAZE_NATIVEMANAGER_CLASS_NAME2 = WAZE_PACKAGE+".main.navigate.c";


    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        loadPreferences();


        // This method is called once per package, so we only want to apply hooks to Waze.
        if (WAZE_PACKAGE.equals(lpparam.packageName)) {
            XposedBridge.log("We are in Waze");
            System.load("/data/data/" + WAZE_PACKAGE + "/lib/libsqlite.so");
            System.load("/data/data/" + WAZE_PACKAGE + "/lib/libwaze.so");
            XposedBridge.log("Loaded native hook");

            //this.hookWazeEnforcement(lpparam.classLoader);

            XposedHelpers.findAndHookMethod(WAZE_NATIVEMANAGER_CLASS_NAME, lpparam.classLoader, WAZE_ENFORCEMENT_METHOD, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Alerts before "+(String) param.getResult());
                }
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    //param.setResult("false");
                    XposedBridge.log("Alerts after "+(String) param.getResult());
                }
            });
            XposedHelpers.findAndHookMethod(WAZE_NATIVEMANAGER_CLASS_NAME, lpparam.classLoader, WAZE_ENFORCEMENT_METHOD2, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Police before "+(String) param.getResult());
                }
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    //param.setResult("false");
                    XposedBridge.log("Police after "+(String) param.getResult());
                }
            });
            XposedHelpers.findAndHookConstructor(WAZE_NATIVEMANAGER_CLASS_NAME2, lpparam.classLoader, WAZE_ENFORCEMENT_METHOD, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Alerts Cons before "+(String) param.getResult());
                }
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    //param.setResult("false");
                    XposedBridge.log("Alerts Cons after "+(String) param.getResult());
                }
            });
        }
    }

    /**

     *
     * @param classLoader ClassLoader for the com.waze package.
     */
    private void hookWazeEnforcement(final ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(WAZE_NATIVEMANAGER_CLASS_NAME, classLoader, WAZE_ENFORCEMENT_METHOD, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("Police "+(String) param.getResult());
                    }
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);

                        param.setResult("false");
                    }

                });
        XposedHelpers.findAndHookMethod(WAZE_NATIVEMANAGER_CLASS_NAME, classLoader, WAZE_ENFORCEMENT_METHOD2, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("Police "+(String) param.getResult());
            }
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);

                param.setResult("false");
            }

        });

    }

    /**
     * Load the preferences from our shared preference file.
     */
    private void loadPreferences() {
        XSharedPreferences prefApps = new XSharedPreferences(PACKAGE_NAME);
        prefApps.makeWorldReadable();

        this.debugMode = prefApps.getBoolean("waze_radar_debug", false);
    }
    /**
     * Capture debugging messages.
     *
     * @param message
     */
    private void debug(String message) {
        if (this.debugMode) {
            String currentDateTime = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
            XposedBridge.log(currentDateTime + ": " + message);
        }
    }
}```
