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
import com.rel.mujde.api.ScriptServer;
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
    private ScriptServer scriptServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_scripts);

        setTitle("Remote Scripts Repository");
        scriptServer = ApiClient.getClient(this);

        listViewRemoteScripts = findViewById(R.id.list_remote_scripts);
        emptyTextView = findViewById(R.id.text_empty_remote_scripts);
        progressBar = findViewById(R.id.progress_bar);

        remoteScriptsAdapter = new RemoteScriptAdapter(this, remoteScriptsList, this);
        listViewRemoteScripts.setAdapter(remoteScriptsAdapter);

        loadRemoteScripts();
    }

    private void loadRemoteScripts() {
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        listViewRemoteScripts.setVisibility(View.GONE);

        scriptServer.getAllScripts().enqueue(new Callback<List<Script>>() {
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
        new AlertDialog.Builder(this)
            .setTitle("Delete Remote Script")
            .setMessage("Are you sure you want to delete " + script.getScriptName() + " from the server?")
            .setPositiveButton("Delete", (dialog, which) -> {
                progressBar.setVisibility(View.VISIBLE);

                scriptServer.deleteScript(script.getScriptId()).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            Toast.makeText(RemoteScriptsActivity.this,
                                "Script deleted successfully", Toast.LENGTH_SHORT).show();
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
        if (script.getContent() == null || script.getContent().isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);

            scriptServer.getScriptById(script.getScriptId()).enqueue(new Callback<Script>() {
                @Override
                public void onResponse(Call<Script> call, Response<Script> response) {
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        Script fullScript = response.body();

                        if (fullScript.getContent() != null && !fullScript.getContent().isEmpty()) {
                            saveScriptFile(fullScript);
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
            saveScriptFile(script);
        }
    }

    private void saveScriptFile(Script script) {
        String scriptName = ScriptUtils.adjustScriptFileName(script.getScriptName());
        File localScriptFile = ScriptUtils.getScriptFile((Context)this, scriptName);

        if (!localScriptFile.exists()) {
            saveScriptIgnoreConflicts(script);
            return;
        }

        // NOTE if got to here, there's a conflict between local and remote scripts. let user choose

        long remoteLastModified = 0;
        long localLastModified = localScriptFile.lastModified();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
            Date remoteDate = sdf.parse(script.getLastModified());
            remoteLastModified = remoteDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String newerVersion = (remoteLastModified > localLastModified) ? "remote" : "local";
        new AlertDialog.Builder(this)
            .setTitle("Script Already Exists")
            .setMessage("A script with the name " + scriptName + " already exists locally. " +
                    "The " + newerVersion + " version is newer. Do you want to override the local script?")
            .setPositiveButton("Override", (dialog, which) -> {
                saveScriptIgnoreConflicts(script);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveScriptIgnoreConflicts(Script script) {
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
