package com.rel.mujde;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rel.mujde.api.ApiClient;
import com.rel.mujde.api.ScriptService;
import com.rel.mujde.api.model.Script;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RemoteScriptsActivity extends AppCompatActivity implements RemoteScriptAdapter.ScriptActionListener {

    private ListView listViewRemoteScripts;
    private TextView emptyTextView;
    private ProgressBar progressBar;
    private List<Script> remoteScriptsList = new ArrayList<>();
    private RemoteScriptAdapter remoteScriptsAdapter;
    private ScriptService scriptService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_scripts);

        // Set title
        setTitle("Remote Scripts Repository");

        // Initialize views
        listViewRemoteScripts = findViewById(R.id.list_remote_scripts);
        emptyTextView = findViewById(R.id.text_empty_remote_scripts);
        progressBar = findViewById(R.id.progress_bar);

        scriptService = ApiClient.getClient(this).create(ScriptService.class);

        // Initialize adapter with this activity as the listener
        remoteScriptsAdapter = new RemoteScriptAdapter(this, remoteScriptsList, this);
        listViewRemoteScripts.setAdapter(remoteScriptsAdapter);

        // Load remote scripts
        loadRemoteScripts();
    }

    private void loadRemoteScripts() {
        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        listViewRemoteScripts.setVisibility(View.GONE);

        // Get API service
        ScriptService scriptService = ApiClient.getClient(this).create(ScriptService.class);

        // Make API call to get all scripts
        scriptService.getAllScripts().enqueue(new Callback<List<Script>>() {
            @Override
            public void onResponse(Call<List<Script>> call, Response<List<Script>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    remoteScriptsList.clear();
                    remoteScriptsList.addAll(response.body());
                    remoteScriptsAdapter.notifyDataSetChanged();

                    if (remoteScriptsList.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        listViewRemoteScripts.setVisibility(View.GONE);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                        listViewRemoteScripts.setVisibility(View.VISIBLE);
                    }
                } else {
                    showErrorDialog("Error: " + response.code());
                    emptyTextView.setVisibility(View.VISIBLE);
                    listViewRemoteScripts.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Script>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                showErrorDialog("Network error: " + t.getMessage());
                emptyTextView.setVisibility(View.VISIBLE);
                listViewRemoteScripts.setVisibility(View.GONE);
            }
        });
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> loadRemoteScripts())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDeleteScript(Script script) {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Remote Script")
                .setMessage("Are you sure you want to delete " + script.getScriptName() + " from the server?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show progress
                    progressBar.setVisibility(View.VISIBLE);

                    // Call API to delete the script
                    scriptService.deleteScript(script.getScriptId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            progressBar.setVisibility(View.GONE);

                            if (response.isSuccessful()) {
                                Toast.makeText(RemoteScriptsActivity.this,
                                        "Script deleted successfully", Toast.LENGTH_SHORT).show();
                                // Refresh the list
                                loadRemoteScripts();
                            } else {
                                showErrorDialog("Failed to delete script: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            showErrorDialog("Network error: " + t.getMessage());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDownloadScript(Script script) {
        // First, check if we already have the content
        if (script.getContent() == null || script.getContent().isEmpty()) {
            // We need to fetch the content first
            progressBar.setVisibility(View.VISIBLE);

            scriptService.getScriptById(script.getScriptId()).enqueue(new Callback<Script>() {
                @Override
                public void onResponse(Call<Script> call, Response<Script> response) {
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        Script fullScript = response.body();
                        if (fullScript.getContent() != null && !fullScript.getContent().isEmpty()) {
                            // Now we have the content, check for conflicts and save
                            checkForConflictAndSave(fullScript);
                        } else {
                            Toast.makeText(RemoteScriptsActivity.this,
                                    "Script content is empty", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showErrorDialog("Failed to get script content: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Script> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    showErrorDialog("Network error: " + t.getMessage());
                }
            });
        } else {
            // We already have the content, check for conflicts and save
            checkForConflictAndSave(script);
        }
    }

    private void checkForConflictAndSave(Script script) {
        String scriptName = ScriptUtils.adjustScriptFileName(script.getScriptName());
        File localScriptFile = ScriptUtils.getScriptFile((Context)this, scriptName);

        if (!localScriptFile.exists()) {
            saveScriptLocally(script);
            return;
        }

        // There's a conflict, we need to compare timestamps and ask the user
        long remoteLastModified = 0;
        long localLastModified = localScriptFile.lastModified();

        // Parse the remote timestamp
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            Date remoteDate = sdf.parse(script.getLastModified());
            remoteLastModified = remoteDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            // TODO: toast? return?
        }

        // Determine which is newer
        String newerVersion = (remoteLastModified > localLastModified) ? "remote" : "local";

        // Show conflict resolution dialog
        new AlertDialog.Builder(this)
            .setTitle("Script Already Exists")
            .setMessage("A script with the name " + scriptName + " already exists locally. " +
                    "The " + newerVersion + " version is newer. Do you want to override the local script?")
            .setPositiveButton("Override", (dialog, which) -> {
                saveScriptLocally(script);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveScriptLocally(Script script) {
        // TODO: cleanup whole file

        String scriptName = ScriptUtils.adjustScriptFileName(script.getScriptName());
        File scriptFile = ScriptUtils.getScriptFile((Context)this, scriptName);

        try (FileWriter writer = new FileWriter(scriptFile)) {
            writer.write(script.getContent());
            AccessibilityUtils.makeFileWorldReadable(scriptFile);

            Toast.makeText(this, "Script downloaded successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving script: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
