package com.rel.mujde;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptSelectionActivity extends AppCompatActivity {
    public static final String EXTRA_PACKAGE_NAME = "extra_package_name";

    private String packageName;
    private List<String> selectedScripts = new ArrayList<>();
    private List<String> availableScripts = new ArrayList<>();
    private ScriptCheckboxAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_selection);

        // Get the package name from the intent
        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName == null) {
            finish();
            return;
        }

        // Load the latest script selections from SharedPreferences instead of intent
        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);
        Map<String, List<String>> appScriptMappings = ScriptUtils.getAllAppScriptMappings(prefs);

        // Get the current scripts for this package
        List<String> currentScripts = appScriptMappings.get(packageName);
        if (currentScripts != null) {
            selectedScripts.addAll(currentScripts);
        }

        // Set up the app name
        TextView appNameText = findViewById(R.id.app_name_text);
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            appNameText.setText(appInfo.loadLabel(packageManager));
        } catch (PackageManager.NameNotFoundException e) {
            appNameText.setText(packageName);
        }

        // Load available scripts
        loadAvailableScripts();

        // Set up the RecyclerView
        RecyclerView scriptsRecyclerView = findViewById(R.id.scripts_recycler_view);
        scriptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create and set the adapter
        adapter = new ScriptCheckboxAdapter(availableScripts, selectedScripts);
        scriptsRecyclerView.setAdapter(adapter);

        // Set up the buttons
        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected scripts from the adapter and filter out non-existent scripts
                List<String> validScripts = filterValidScripts(adapter.getSelectedScripts());

                // Save to SharedPreferences
                SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);

                // Always clear the original mapping and build from scratch
                Map<String, List<String>> appScriptMappings = new HashMap<>();

                // Add the current selection for this package
                if (!validScripts.isEmpty()) {
                    appScriptMappings.put(packageName, validScripts);
                }

                // Save the updated mappings
                ScriptUtils.saveAppScriptMappings(prefs, appScriptMappings);

                // Create the result intent
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_PACKAGE_NAME, packageName);

                // Set the result and finish
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    /**
     * get a reduced list where all script-files are ensured to exist and be valid
     */
    private List<String> filterValidScripts(List<String> scriptNames) {
        List<String> validScripts = new ArrayList<>();

        for (String scriptName : scriptNames) {
            File scriptFile = ScriptUtils.getScriptFile((Context)this, scriptName);
            if (scriptFile.exists() && scriptFile.isFile()) {
                validScripts.add(scriptName);
            }
        }

        return validScripts;
    }

    private void loadAvailableScripts() {
        availableScripts.clear();
        List<String> validSelectedScripts = filterValidScripts(selectedScripts);

        // Update selectedScripts with only valid scripts
        selectedScripts.clear();
        selectedScripts.addAll(validSelectedScripts);

        File[] scriptFiles = ScriptUtils.getScripts(this);
        if (scriptFiles == null || scriptFiles.length == 0) {
            return;
        }

        for (File file : scriptFiles) {
            availableScripts.add(file.getName());

            // TODO: use chmod util-cls
            try {
                Process chmod = Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
                chmod.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
