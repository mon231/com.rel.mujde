package com.rel.mujde;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ScriptUtils {
    public static void saveAppScriptMappings(SharedPreferences prefs, Map<String, List<String>> appScriptMappings) {
        try {
            JSONObject jsonObject = new JSONObject();

            for (Map.Entry<String, List<String>> entry : appScriptMappings.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }

                JSONArray scriptsArray = new JSONArray();
                for (String script : entry.getValue()) {
                    scriptsArray.put(script);
                }

                jsonObject.put(entry.getKey(), scriptsArray);
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Constants.PREF_APP_SCRIPTS_MAP, jsonObject.toString());
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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

    @Nullable
    public static String getRepositoryAddress(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        return prefs.getString(Constants.PREF_SCRIPTS_REPOSITORY, null);
    }

    public static boolean hasSetRepositoryAddress(Context context) {
        String repository = getRepositoryAddress(context);
        return repository != null && !repository.isEmpty();
    }

    // NOTE XSharedPreferences inherits from SharedPreferences, this function is used for both
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
