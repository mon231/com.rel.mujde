package com.rel.mujde;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
    private String packageName;
    private ScriptCheckboxAdapter adapter;

    // NOTE these must be final as the adapter uses their reference
    private final List<String> selectedScripts = new ArrayList<>();
    private final List<String> availableScripts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_selection);

        TextView appNameText = findViewById(R.id.app_name_text);
        RecyclerView scriptsRecyclerView = findViewById(R.id.scripts_recycler_view);

        packageName = getIntent().getStringExtra(Constants.INTENT_REQUEST_PACKAGE_NAME);
        if (packageName == null || packageName.isEmpty()) {
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);
        List<String> currentScripts = ScriptUtils.getAllAppScriptMappings(prefs).get(packageName);

        if (currentScripts != null) {
            selectedScripts.addAll(currentScripts);
        }

        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            appNameText.setText(appInfo.loadLabel(packageManager));
        } catch (PackageManager.NameNotFoundException e) {
            appNameText.setText(packageName);
        }

        loadAvailableScripts();
        scriptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ScriptCheckboxAdapter(availableScripts, selectedScripts);
        scriptsRecyclerView.setAdapter(adapter);

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
                List<String> validScripts = filterValidScripts(adapter.getSelectedScripts());
                SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, MODE_WORLD_READABLE);
                ScriptUtils.saveAppScriptMappings(prefs, packageName, validScripts);

                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

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

        selectedScripts.clear();
        selectedScripts.addAll(validSelectedScripts);

        File[] scriptFiles = ScriptUtils.getScripts(this);
        if (scriptFiles == null || scriptFiles.length == 0) {
            return;
        }

        for (File file : scriptFiles) {
            availableScripts.add(file.getName());
            AccessibilityUtils.makeFileWorldReadable(file);
        }
    }
}
