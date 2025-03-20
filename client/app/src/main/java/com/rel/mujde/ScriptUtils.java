package com.rel.mujde;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.robv.android.xposed.XSharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    
    /**
     * Get scripts for a specific app from SharedPreferences
     * @param prefs SharedPreferences instance
     * @param packageName Package name of the app
     * @return List of script names for the app
     */
    public static List<String> getScriptsForApp(SharedPreferences prefs, String packageName) {
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
    
    /**
     * Get scripts for a specific package from XSharedPreferences (for Xposed module)
     * @param packageName Package name of the app
     * @param prefs XSharedPreferences instance
     * @return List of script names for the app
     */
    public static List<String> getScriptsForPackage(String packageName, XSharedPreferences prefs) {
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
