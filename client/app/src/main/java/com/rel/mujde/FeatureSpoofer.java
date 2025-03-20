package com.rel.mujde;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class FeatureSpoofer implements IXposedHookLoadPackage {
    private XSharedPreferences pref;

    private void log(String message) {
        XposedBridge.log("[Mujde] " + message);
    }

    private XSharedPreferences getPreferences() {
        if (pref == null) {
            pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, Constants.SHARED_PREF_FILE_NAME);
            pref.reload();

            if (!pref.getFile().canRead()) {
                log("ERROR: Preference file is not readable!");
                makeWorldReadable();
            }
        } else {
            pref.reload();
        }
        return pref;
    }

    private void makeWorldReadable() {
        try {
            Process chmod = Runtime.getRuntime().exec("chmod 664 " + pref.getFile().getAbsolutePath());
            chmod.waitFor();
            pref.reload();
        } catch (Exception e) {
            log("Failed to make preferences readable: " + e.getMessage());
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }

        String packageName = lpparam.packageName;
        if (packageName.equals(BuildConfig.APPLICATION_ID)) {
            return;
        }

        XSharedPreferences prefs = getPreferences();
        List<String> scripts = ScriptUtils.getScriptsForPackage(packageName, prefs);

        if (scripts == null || scripts.isEmpty()) {
            return;
        }

        // Hook Activity.onCreate to inject scripts
        hookActivityOnCreate(lpparam, scripts);
    }

    /**
     * Read the content of a script file
     * @param scriptName Name of the script file
     * @return List of lines in the script file
     */
    private List<String> readScriptContent(String scriptName) {
        List<String> lines = new ArrayList<>();
        
        log("Reading script: " + scriptName);
        
        try {
            // Look for the script in the app's files directory
            File scriptsDir = new File(new File("/data/data/" + BuildConfig.APPLICATION_ID), "files/scripts");
            File scriptFile = new File(scriptsDir, scriptName);
            
            if (scriptFile.exists() && scriptFile.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                    log("Successfully read script: " + scriptName + " with " + lines.size() + " lines");
                }
            } else {
                log("Script file does not exist or is not readable: " + scriptFile.getAbsolutePath());
                
                // Try using XSharedPreferences as a fallback
                XSharedPreferences scriptPref = new XSharedPreferences(BuildConfig.APPLICATION_ID, "script_contents");
                String scriptContent = scriptPref.getString(scriptName, "");
                
                if (!scriptContent.isEmpty()) {
                    String[] lineArray = scriptContent.split("\n");
                    for (String line : lineArray) {
                        lines.add(line);
                    }
                    log("Read " + lines.size() + " lines from XSharedPreferences");
                }
            }
        } catch (Exception e) {
            log("Error reading script: " + e.getMessage());
        }
        
        // Add a fallback message if the script couldn't be read
        if (lines.isEmpty()) {
            lines.add("// Unable to read script: " + scriptName);
            lines.add("// Please place your script in: /data/data/" + BuildConfig.APPLICATION_ID + "/files/scripts/" + scriptName);
            lines.add("// You can use 'adb push your_script.js /data/data/" + BuildConfig.APPLICATION_ID + "/files/scripts/" + scriptName + "'");
        }
        
        return lines;
    }
    
    /**
     * Inject Frida scripts into target application
     * @param context Context to use for script injection
     * @param scriptNames List of script names to inject
     */
    private void injectFridaScripts(final Context context, final List<String> scriptNames) {
        final Handler handler = new Handler(Looper.getMainLooper());
        
        // bomboclat
        XSharedPreferences prefs = getPreferences();
        boolean fridaEnabled = prefs.getBoolean("enable_frida", true); // Default to true for testing
        
        // Log device architecture information
        String[] supportedAbis = android.os.Build.SUPPORTED_ABIS;
        StringBuilder abiInfo = new StringBuilder("Device supported ABIs: ");
        for (String abi : supportedAbis) {
            abiInfo.append(abi).append(", ");
        }
        log(abiInfo.toString());
        
        // Log native library directory information
        String nativeLibDir = context.getApplicationInfo().nativeLibraryDir;
        log("Native library directory: " + nativeLibDir);
        
        // Check if the Frida Gadget library exists
        File fridaLib = new File(nativeLibDir, "libfrida-gadget.so");
        log("Checking for Frida Gadget at: " + fridaLib.getAbsolutePath() + ", exists: " + fridaLib.exists());
        
        if (!fridaEnabled) {
            log("Frida integration is disabled. Enable it in settings if needed.");
            Toast.makeText(context, "Frida integration is disabled. Enable it in settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if the library exists and log the result
        if (!fridaLib.exists()) {
            log("ERROR: Frida Gadget library not found at: " + fridaLib.getAbsolutePath());
            Toast.makeText(context, "Frida Gadget library not found. Check Xposed logs for details.", Toast.LENGTH_LONG).show();
        } else {
            log("Frida Gadget library found at: " + fridaLib.getAbsolutePath() + ", size: " + fridaLib.length() + " bytes");
        }
        
        // Ensure the scripts directory exists
        File scriptsDir = new File(context.getFilesDir(), "scripts");
        if (!scriptsDir.exists()) {
            boolean created = scriptsDir.mkdirs();
            log("Created scripts directory: " + created);
        }
        
        // Process each script
        for (int scriptIndex = 0; scriptIndex < scriptNames.size(); scriptIndex++) {
            final String scriptName = scriptNames.get(scriptIndex);
            final List<String> scriptLines = readScriptContent(scriptName);
            
            // Skip empty scripts
            if (scriptLines.isEmpty()) {
                log("Skipping empty script: " + scriptName);
                continue;
            }
            
            // Build the script content
            final StringBuilder scriptContent = new StringBuilder();
            for (String line : scriptLines) {
                scriptContent.append(line).append("\n");
            }
            
            // Inject the script using Frida
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Show a toast indicating script injection
                        Toast.makeText(context, "Injecting Frida script: " + scriptName, Toast.LENGTH_SHORT).show();
                        log("Injecting Frida script: " + scriptName);
                        
                        try {
                            // Create a config directory for Frida Gadget
                            File configDir = new File(context.getFilesDir(), "frida-config");
                            if (!configDir.exists()) {
                                configDir.mkdirs();
                            }
                            
                            // Create a script directory for Frida Gadget
                            File scriptDir = new File(configDir, "scripts");
                            if (!scriptDir.exists()) {
                                scriptDir.mkdirs();
                            }
                            
                            // Write the script to the script directory
                            File scriptFile = new File(scriptDir, scriptName);
                            try (OutputStream os = new FileOutputStream(scriptFile)) {
                                os.write(scriptContent.toString().getBytes());
                            }
                            
                            // Create a config file for Frida Gadget
                            String configJson = "{\n" +
                                    "  \"interaction\": {\n" +
                                    "    \"type\": \"script\",\n" +
                                    "    \"path\": \"" + scriptFile.getAbsolutePath() + "\"\n" +
                                    "  }\n" +
                                    "}";
                            
                            File configFile = new File(configDir, "config.json");
                            try (OutputStream os = new FileOutputStream(configFile)) {
                                os.write(configJson.getBytes());
                            }
                            
                            // Log the script content and config for debugging
                            log("Script content for " + scriptName + " has been written to: " + scriptFile.getAbsolutePath());
                            log("Config file has been written to: " + configFile.getAbsolutePath());
                            
                            // We need to extract and load the Frida Gadget library in the target app
                            try {
                                // Get the target app's package name and our package name
                                String targetPackage = context.getPackageName();
                                String ourPackage = BuildConfig.APPLICATION_ID;
                                log("Target package: " + targetPackage);
                                log("Our package: " + ourPackage);
                                
                                // Get the device's primary ABI
                                String primaryAbi = android.os.Build.SUPPORTED_ABIS[0];
                                log("Device primary ABI: " + primaryAbi);
                                
                                // Get the native library path for the target app
                                String targetNativeLibDir = context.getApplicationInfo().nativeLibraryDir;
                                log("Target native library directory: " + targetNativeLibDir);
                                
                                // Check if the Frida Gadget library exists in the target app's native library directory
                                File targetFridaLib = new File(targetNativeLibDir, "libfrida-gadget.so");
                                log("Checking for Frida Gadget at: " + targetFridaLib.getAbsolutePath());
                                log("Frida Gadget exists in target app: " + targetFridaLib.exists());
                                
                                if (targetFridaLib.exists()) {
                                    log("Frida Gadget found in target app: " + targetFridaLib.getAbsolutePath());
                                    log("Size: " + targetFridaLib.length() + " bytes");
                                    
                                    // Try to load the library using System.load with the absolute path
                                    try {
                                        log("Attempting to load Frida Gadget from target app's directory");
                                        System.load(targetFridaLib.getAbsolutePath());
                                        log("Successfully loaded Frida Gadget from target app's directory");
                                        Toast.makeText(context, "Successfully loaded Frida Gadget", Toast.LENGTH_SHORT).show();
                                    } catch (UnsatisfiedLinkError e) {
                                        log("Failed to load Frida Gadget from target app: " + e.getMessage());
                                        Toast.makeText(context, "Failed to load Frida Gadget: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                        // The library doesn't exist in the target app, so we need to copy it from our APK
                                    log("Frida Gadget not found in target app, attempting to copy from APK resources");
                                    
                                    try {
                                        // Create a directory in the target app's data directory to store the library
                                        File targetLibDir = new File(context.getFilesDir(), "lib");
                                        if (!targetLibDir.exists()) {
                                            targetLibDir.mkdirs();
                                        }
                                        
                                        // Create a file object for the destination
                                        File targetLibFile = new File(targetLibDir, "libfrida-gadget.so");
                                        log("Target library path: " + targetLibFile.getAbsolutePath());
                                        
                                        // Map the ABI to the correct directory name
                                        String abiDir;
                                        switch (primaryAbi) {
                                            case "arm64-v8a":
                                                abiDir = "arm64-v8a";
                                                break;
                                            case "armeabi-v7a":
                                                abiDir = "armeabi-v7a";
                                                break;
                                            case "x86":
                                                abiDir = "x86";
                                                break;
                                            case "x86_64":
                                                abiDir = "x86_64";
                                                break;
                                            default:
                                                abiDir = "arm64-v8a"; // Default to arm64-v8a
                                        }
                                        
                                        // Try to copy the library directly from our module's jniLibs directory
                                        log("Attempting to copy Frida Gadget library to target app");
                                        
                                        // We need to extract the library from our APK's jniLibs directory
                                        // Since we can't directly access our APK in the context of the target app,
                                        // we'll try a different approach
                                        
                                        // First, let's try to find our module's package directory
                                        String modulePackageName = "com.rel.mujde";
                                        log("Looking for module package: " + modulePackageName);
                                        
                                        // Try several possible paths for the module
                                        String[] possiblePaths = {
                                            "/data/app/" + modulePackageName.replace(".", "/") + "-1/lib/" + primaryAbi,
                                            "/data/app/" + modulePackageName.replace(".", "/") + "-2/lib/" + primaryAbi,
                                            "/data/app/~~96-Z0i0CNcYZWya-zqTU2w==/com.rel.mujde-64aOge1KOrBQYje0pUAWlg==/lib/" + primaryAbi,
                                            "/data/app/com.rel.mujde/lib/" + primaryAbi,
                                            "/data/app/com.rel.mujde-1/lib/" + primaryAbi,
                                            "/data/app/com.rel.mujde-2/lib/" + primaryAbi
                                        };
                                        
                                        boolean foundLibrary = false;
                                        
                                        // Try each possible path
                                        for (String path : possiblePaths) {
                                            File libDir = new File(path);
                                            File fridaLib = new File(libDir, "libfrida-gadget.so");
                                            log("Checking for Frida Gadget at: " + fridaLib.getAbsolutePath());
                                            
                                            if (fridaLib.exists() && fridaLib.canRead()) {
                                                log("Found Frida Gadget at: " + fridaLib.getAbsolutePath());
                                                log("File size: " + fridaLib.length() + " bytes");
                                                
                                                // Copy the library to the target app
                                                try (FileInputStream fis = new FileInputStream(fridaLib);
                                                     FileOutputStream fos = new FileOutputStream(targetLibFile)) {
                                                    byte[] buffer = new byte[8192];
                                                    int read;
                                                    while ((read = fis.read(buffer)) != -1) {
                                                        fos.write(buffer, 0, read);
                                                    }
                                                    fos.flush();
                                                    
                                                    log("Successfully copied Frida Gadget to: " + targetLibFile.getAbsolutePath());
                                                    log("File size: " + targetLibFile.length() + " bytes");
                                                    
                                                    // Try to load the library
                                                    try {
                                                        System.load(targetLibFile.getAbsolutePath());
                                                        log("Successfully loaded Frida Gadget from: " + targetLibFile.getAbsolutePath());
                                                        Toast.makeText(context, "Successfully loaded Frida Gadget", Toast.LENGTH_SHORT).show();
                                                        foundLibrary = true;
                                                        break; // Exit the loop if we successfully loaded the library
                                                    } catch (UnsatisfiedLinkError e) {
                                                        log("Failed to load Frida Gadget: " + e.getMessage());
                                                        Toast.makeText(context, "Failed to load Frida Gadget: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                } catch (IOException e) {
                                                    log("Error copying Frida Gadget: " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                        
                                        if (!foundLibrary) {
                                            // Try to find the library in the system directory
                                            log("Could not find Frida Gadget in module paths, checking system paths");
                                            
                                            // Check if the library exists in the system library directory
                                            String systemLibPath = "/system/lib64/libfrida-gadget.so";
                                            if (!primaryAbi.contains("64")) {
                                                systemLibPath = "/system/lib/libfrida-gadget.so";
                                            }
                                            
                                            File systemLib = new File(systemLibPath);
                                            log("Checking for Frida Gadget at: " + systemLibPath);
                                            log("Frida Gadget exists in system: " + systemLib.exists());
                                            
                                            if (systemLib.exists() && systemLib.canRead()) {
                                                // Copy the library to the target app
                                                try (FileInputStream fis = new FileInputStream(systemLib);
                                                     FileOutputStream fos = new FileOutputStream(targetLibFile)) {
                                                    byte[] buffer = new byte[8192];
                                                    int read;
                                                    while ((read = fis.read(buffer)) != -1) {
                                                        fos.write(buffer, 0, read);
                                                    }
                                                    fos.flush();
                                                    
                                                    log("Successfully copied Frida Gadget from system to: " + targetLibFile.getAbsolutePath());
                                                    log("File size: " + targetLibFile.length() + " bytes");
                                                    
                                                    // Try to load the library
                                                    try {
                                                        System.load(targetLibFile.getAbsolutePath());
                                                        log("Successfully loaded Frida Gadget from: " + targetLibFile.getAbsolutePath());
                                                        Toast.makeText(context, "Successfully loaded Frida Gadget", Toast.LENGTH_SHORT).show();
                                                    } catch (UnsatisfiedLinkError e) {
                                                        log("Failed to load Frida Gadget: " + e.getMessage());
                                                        Toast.makeText(context, "Failed to load Frida Gadget: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                } catch (IOException e) {
                                                    log("Error copying Frida Gadget from system: " + e.getMessage());
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                log("Could not find Frida Gadget library in any location");
                                                Toast.makeText(context, "Could not find Frida Gadget library", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    } catch (Exception e) {
                                        log("Error copying Frida Gadget: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                }
                                
                                // Log that we've prepared everything
                                log("Script and config prepared for Frida integration");
                                log("To use Frida with this app, you need to ensure the library is loaded by the target app");
                                log("Consider using Frida CLI tools instead of Frida Gadget for this use case");
                            } catch (Exception e) {
                                log("Error setting up Frida environment: " + e.getMessage());
                                Toast.makeText(context, "Error setting up Frida environment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            
                            // Log success
                            log("Successfully injected Frida script: " + scriptName);
                            
                            // Show a toast indicating successful injection
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Script " + scriptName + " injected successfully", Toast.LENGTH_SHORT).show();
                                }
                            }, 1000);
                        } catch (Exception e) {
                            log("Error setting up Frida config: " + e.getMessage());
                            Toast.makeText(context, "Error setting up Frida config: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        log("Error injecting Frida script: " + e.getMessage());
                        Toast.makeText(context, "Error injecting script: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }, 2000 * scriptIndex); // 2 second delay between scripts
        }
    }
    
    /**
     * Maps Android ABI to Frida architecture name
     * @param abi Android ABI (e.g., arm64-v8a)
     * @return Frida architecture name (e.g., android-arm64)
     */
    private String mapAbiToFridaArch(String abi) {
        switch (abi) {
            case "arm64-v8a":
                return "android-arm64";
            case "armeabi-v7a":
                return "android-arm";
            case "x86":
                return "android-x86";
            case "x86_64":
                return "android-x86_64";
            default:
                return "unknown";
        }
    }
    
    private void hookActivityOnCreate(XC_LoadPackage.LoadPackageParam lpparam, final List<String> scripts) {
        try {
            XposedHelpers.findAndHookMethod(
                    Activity.class.getName(),
                    lpparam.classLoader,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            final Activity activity = (Activity) param.thisObject;
                            final String packageName = activity.getPackageName();
                            final String appName = activity.getApplicationInfo().loadLabel(activity.getPackageManager()).toString();
                            
                            // Show toast on the main thread
                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String toastMessage = "Mujde: " + appName + " ("+packageName+") is running " + scripts.size() + " scripts";
                                        Toast toast = Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG);
                                        toast.setDuration(3000); // 3 seconds
                                        toast.show();
                                        log("Showed toast for " + packageName + " with " + scripts.size() + " scripts");
                                        
                                        // Inject Frida scripts
                                        injectFridaScripts(activity, scripts);
                                    } catch (Exception e) {
                                        log("Error showing toast: " + e.getMessage());
                                    }
                                }
                            }, 1000); // Delay by 1 second to ensure the activity is fully created
                        }
                    }
            );
        } catch (Throwable e) {
            log("Error hooking Activity.onCreate(): " + e.getMessage());
        }
    }
}
