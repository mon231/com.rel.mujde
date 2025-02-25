package com.rel.mujde;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout packageNameLayout;
    private TextInputLayout messageLayout;
    private TextInputEditText packageNameInput;
    private TextInputEditText messageInput;
    private MaterialButton addButton;
    private RecyclerView configList;
    private ConfigAdapter adapter;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ToastConfigs";
    private static final String CONFIG_KEY = "configs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar without using setSupportActionBar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Toast Configuration");

        // Initialize views
        packageNameLayout = findViewById(R.id.packageNameLayout);
        messageLayout = findViewById(R.id.messageLayout);
        packageNameInput = findViewById(R.id.packageName);
        messageInput = findViewById(R.id.message);
        addButton = findViewById(R.id.addButton);
        configList = findViewById(R.id.configList);

        // Set up RecyclerView
        configList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConfigAdapter();
        configList.setAdapter(adapter);

        // Load saved configurations
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadConfigs();

        // Set up click listeners
        addButton.setOnClickListener(v -> addConfiguration());

        adapter.setOnConfigClickListener(config -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Configuration")
                    .setMessage("Do you want to delete this configuration?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        List<ConfigAdapter.Config> configs = new ArrayList<>(adapter.getConfigs());
                        configs.remove(config);
                        adapter.setConfigs(configs);
                        saveConfigs();
                        
                        // Animate the RecyclerView
                        configList.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void addConfiguration() {
        String packageName = packageNameInput.getText().toString().trim();
        String message = messageInput.getText().toString().trim();

        // Validate input
        boolean hasError = false;
        if (TextUtils.isEmpty(packageName)) {
            packageNameLayout.setError("Package name cannot be empty");
            hasError = true;
        } else {
            packageNameLayout.setError(null);
        }

        if (TextUtils.isEmpty(message)) {
            messageLayout.setError("Message cannot be empty");
            hasError = true;
        } else {
            messageLayout.setError(null);
        }

        if (hasError) {
            return;
        }

        // Add new configuration
        List<ConfigAdapter.Config> configs = new ArrayList<>(adapter.getConfigs());
        configs.add(new ConfigAdapter.Config(packageName, message));
        adapter.setConfigs(configs);
        saveConfigs();

        // Clear inputs
        packageNameInput.setText("");
        messageInput.setText("");

        // Show success message
        Toast.makeText(this, "Configuration added successfully", Toast.LENGTH_SHORT).show();

        // Animate the RecyclerView
        configList.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
    }

    private void loadConfigs() {
        String json = prefs.getString(CONFIG_KEY, "[]");
        Type type = new TypeToken<List<ConfigAdapter.Config>>(){}.getType();
        List<ConfigAdapter.Config> configs = new Gson().fromJson(json, type);
        adapter.setConfigs(configs != null ? configs : new ArrayList<>());
    }

    private void saveConfigs() {
        String json = new Gson().toJson(adapter.getConfigs());
        prefs.edit().putString(CONFIG_KEY, json).apply();
    }
}
