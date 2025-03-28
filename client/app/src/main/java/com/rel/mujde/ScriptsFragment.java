package com.rel.mujde;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptsFragment extends Fragment {
    private ListView listViewScripts;
    private TextView emptyTextView;
    private FloatingActionButton fabAddScript;
    private List<String> scriptsList = new ArrayList<>();
    private ArrayAdapter<String> scriptsAdapter;

    public ScriptsFragment() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // TODO: remove this function?
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scripts, container, false);

        listViewScripts = view.findViewById(R.id.list_scripts);
        emptyTextView = view.findViewById(R.id.text_empty_scripts);
        fabAddScript = view.findViewById(R.id.fab_add_script);

        // Initialize the custom adapter with edit and delete buttons
        scriptsAdapter = new ScriptAdapter(requireContext(), scriptsList, new ScriptAdapter.ScriptActionListener() {
            @Override
            public void onEditScript(String scriptName) {
                openScriptEditor(scriptName);
            }

            @Override
            public void onDeleteScript(String scriptName) {
                showDeleteScriptDialog(scriptName);
            }
        });
        listViewScripts.setAdapter(scriptsAdapter);

        // Set up click listeners
        setupListeners(view);

        // Load scripts
        loadScripts();

        return view;
    }

    private void setupListeners(View view) {
        // FAB click listener to add a new script
        fabAddScript.setOnClickListener(v -> showAddScriptDialog());

        // Cloud FAB click listener to open remote scripts activity
        FloatingActionButton fabCloud = view.findViewById(R.id.fab_cloud);
        fabCloud.setOnClickListener(v -> {
            // Check if repository is configured
            SharedPreferences prefs = requireContext().getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
            String repository = prefs.getString(Constants.PREF_SCRIPTS_REPOSITORY, null);

            if (repository == null || repository.isEmpty()) {
                Toast.makeText(requireContext(), "Please configure repository URL in Home tab first", Toast.LENGTH_LONG).show();
                return;
            }

            // Open remote scripts activity
            Intent intent = new Intent(requireContext(), RemoteScriptsActivity.class);
            startActivity(intent);
        });

        // No longer using click/long-click listeners on list items
        // Edit and delete functionality is now handled by buttons in each list item
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the scripts list when the fragment becomes visible
        loadScripts();
    }

    private void loadScripts() {
        scriptsList.clear();
        // TODO: cleanup, chmod
        // Set directory permissions to 755 (rwxr-xr-x)
        try {
            Runtime.getRuntime()
                .exec("chmod 755 " + ScriptUtils.getScriptsDirectoryPath(requireContext()))
                .waitFor();

            String dataDir = requireContext().getApplicationInfo().dataDir;
            File prefsDir = new File(dataDir, "shared_prefs");

            if (!prefsDir.exists()) {
                prefsDir.mkdirs();
            }

            Runtime.getRuntime()
                .exec("chmod 644 " + prefsDir.getAbsolutePath())
                .waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File[] files = ScriptUtils.getScripts(requireContext());
        if (files != null && files.length > 0) {
            for (File file : files) {
                scriptsList.add(file.getName());

                // Set file permissions to 644 (rw-r--r--)
                try {
                    Runtime.getRuntime()
                        .exec("chmod 644 " + file.getAbsolutePath())
                        .waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            emptyTextView.setVisibility(View.GONE);
            listViewScripts.setVisibility(View.VISIBLE);
        } else {
            emptyTextView.setVisibility(View.VISIBLE);
            listViewScripts.setVisibility(View.GONE);
        }

        scriptsAdapter.notifyDataSetChanged();
    }

    private void showAddScriptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Script");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("script_name" + Constants.SCRIPT_FILE_EXT);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String fileName = input.getText().toString().trim();

            if (fileName.isEmpty()) {
                Toast.makeText(requireContext(), "File name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            fileName = ScriptUtils.adjustScriptFileName(fileName);
            createNewScript(fileName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createNewScript(String fileName) {
        File scriptFile = ScriptUtils.getScriptFile(requireContext(), fileName);

        if (scriptFile.exists()) {
            Toast.makeText(requireContext(),
                "A script with this name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!scriptFile.createNewFile()) {
                Toast.makeText(requireContext(), "Failed to create script", Toast.LENGTH_SHORT).show();
                return;
            }

            String templateContent = "// My cool frida script\n";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
                writer.write(templateContent);
            }

            // Set file permissions to 644 (rw-r--r--)
            try {
                Runtime.getRuntime()
                    .exec("chmod 644 " + scriptFile.getAbsolutePath())
                    .waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Toast.makeText(requireContext(), "Script created successfully", Toast.LENGTH_SHORT).show();
            loadScripts(); // Refresh the list

            openScriptEditor(fileName);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openScriptEditor(String scriptName) {
        File scriptFile = ScriptUtils.getScriptFile(requireContext(), scriptName);

        if (!scriptFile.exists()) {
            Toast.makeText(requireContext(), "Script file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read the script content
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error reading script: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a dialog with an EditText to edit the script
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Script: " + scriptName);

        // Set up the input field
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(content.toString());
        input.setTextDirection(View.TEXT_DIRECTION_LTR);
        input.setMinLines(10);
        input.setMaxLines(20);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newContent = input.getText().toString();
            saveScript(scriptName, newContent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveScript(String scriptName, String content) {
        File scriptFile = ScriptUtils.getScriptFile(requireContext(), scriptName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
            writer.write(content);

            try {
                // TODO: note Set file permissions to 644 (rw-r--r--)
                Runtime.getRuntime()
                    .exec("chmod 644 " + scriptFile.getAbsolutePath())
                    .waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Toast.makeText(requireContext(), "Script saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error saving script: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteScriptDialog(String scriptName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Script")
                .setMessage("Are you sure you want to delete " + scriptName + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteScript(scriptName))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteScript(String scriptName) {
        File scriptFile = ScriptUtils.getScriptFile(requireContext(), scriptName);

        if (scriptFile.exists()) {
            if (scriptFile.delete()) {
                Toast.makeText(requireContext(), "Script deleted", Toast.LENGTH_SHORT).show();
                loadScripts(); // Refresh the list
            } else {
                Toast.makeText(requireContext(), "Failed to delete script", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Script not found", Toast.LENGTH_SHORT).show();
        }
    }
}
