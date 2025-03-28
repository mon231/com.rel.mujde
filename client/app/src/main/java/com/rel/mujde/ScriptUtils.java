package com.rel.mujde;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// TODO: cleanup

public class ScriptUtils {

    /**
     * Save app script mappings to SharedPreferences
     * @param prefs SharedPreferences instance
     * @param appScriptMappings Map of package names to lists of script names
     */
    public static void saveAppScriptMappings(SharedPreferences prefs, Map<String, List<String>> appScriptMappings) {
        try {
            JSONObject jsonObject = new JSONObject();

            // Only save apps that have selected scripts
            for (Map.Entry<String, List<String>> entry : appScriptMappings.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    JSONArray scriptsArray = new JSONArray();
                    for (String script : entry.getValue()) {
                        scriptsArray.put(script);
                    }
                    jsonObject.put(entry.getKey(), scriptsArray);
                }
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.PREF_APP_SCRIPTS_MAP, jsonObject.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get all app script mappings from SharedPreferences
     * @param prefs SharedPreferences instance
     * @return Map of package names to lists of script names
     */
    public static Map<String, List<String>> getAllAppScriptMappings(SharedPreferences prefs) {
        Map<String, List<String>> appScriptMappings = new HashMap<>();

        try {
            String jsonString = prefs.getString(Constants.PREF_APP_SCRIPTS_MAP, "{}");
            JSONObject jsonObject = new JSONObject(jsonString);

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String packageName = keys.next();
                JSONArray scriptsArray = jsonObject.getJSONArray(packageName);

                List<String> scripts = new ArrayList<>();
                for (int i = 0; i < scriptsArray.length(); i++) {
                    scripts.add(scriptsArray.getString(i));
                }

                appScriptMappings.put(packageName, scripts);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return appScriptMappings;
    }

    public static File[] getScripts(Context context) {
        return getScriptsDirectory(context).listFiles(file ->
            file.isFile() && file.getName().endsWith(Constants.SCRIPT_FILE_EXT));
    }

    public static File getScriptsDirectory(Context context) {
        File scriptsDir = new File(context.getFilesDir(), Constants.SCRIPTS_DIRECTORY_NAME);

        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }

        return scriptsDir;
    }

    public static File getScriptFile(Context context, String scriptName) {
        return new File(getScriptsDirectory(context), scriptName);
    }

    public static String getScriptsDirectoryPath(Context context) {
        return getScriptsDirectory(context).getAbsolutePath();
    }

    public static String adjustScriptFileName(String scriptName) {
        if (scriptName.endsWith(Constants.SCRIPT_FILE_EXT)) {
            return scriptName;
        }

        return scriptName + Constants.SCRIPT_FILE_EXT;
    }

    /**
     * NOTE that XSharedPreferences inherits from SharedPreferences,
     * and this function is used with both types of arguments
     */
    public static List<String> getScriptsForPackage(String packageName, SharedPreferences prefs) {
        List<String> scripts = new ArrayList<>();

        try {
            String jsonString = prefs.getString(Constants.PREF_APP_SCRIPTS_MAP, "{}");
            JSONObject jsonObject = new JSONObject(jsonString);

            if (jsonObject.has(packageName)) {
                JSONArray scriptsArray = jsonObject.getJSONArray(packageName);

                for (int i = 0; i < scriptsArray.length(); i++) {
                    scripts.add(scriptsArray.getString(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return scripts;
    }
}
