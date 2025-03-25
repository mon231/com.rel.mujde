package com.rel.mujde;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class HomeFragment extends Fragment {
    private SharedPreferences sharedPreferences;
    private TextInputEditText repositoryInput;
    private TextInputLayout repositoryInputLayout;
    private Button saveRepositoryButton;
    private boolean isValidating = false;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
        
        // Initialize views
        repositoryInputLayout = view.findViewById(R.id.repository_input_layout);
        repositoryInput = view.findViewById(R.id.repository_input);
        saveRepositoryButton = view.findViewById(R.id.save_repository_button);
        
        // Load saved repository URL or set default
        String savedRepository = sharedPreferences.getString(Constants.PREF_SCRIPTS_REPOSITORY, Constants.DEFAULT_REPOSITORY);
        repositoryInput.setText(savedRepository);
        
        // Set up save button click listener
        saveRepositoryButton.setOnClickListener(v -> saveRepositoryUrl());
        
        return view;
    }
    
    private void saveRepositoryUrl() {
        if (isValidating) {
            return; // Prevent multiple validations at once
        }
        
        String repositoryUrl = repositoryInput.getText().toString().trim();
        
        // Validate repository URL format (address:port)
        if (!isValidRepositoryFormat(repositoryUrl)) {
            repositoryInputLayout.setError("Invalid format. Please use address:port format");
            return;
        }
        
        // Reset error
        repositoryInputLayout.setError(null);
        
        // Extract address and port
        String[] parts = repositoryUrl.split(":");
        String address = parts[0];
        int port;
        
        try {
            port = Integer.parseInt(parts[1]);
            if (port <= 0 || port > 65535) {
                repositoryInputLayout.setError("Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            repositoryInputLayout.setError("Invalid port number");
            return;
        }
        
        // Show loading state
        saveRepositoryButton.setEnabled(false);
        saveRepositoryButton.setText("Validating...");
        isValidating = true;
        
        // Validate connectivity in background
        new ValidateRepositoryTask(address, port, new ValidationCallback() {
            @Override
            public void onValidationComplete(boolean isValid, String errorMessage) {
                // Return to UI thread
                requireActivity().runOnUiThread(() -> {
                    saveRepositoryButton.setEnabled(true);
                    saveRepositoryButton.setText("Save");
                    isValidating = false;
                    
                    if (isValid) {
                        // Save to SharedPreferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(Constants.PREF_SCRIPTS_REPOSITORY, repositoryUrl);
                        editor.apply();
                        
                        Toast.makeText(requireContext(), "Repository URL saved", Toast.LENGTH_SHORT).show();
                    } else {
                        repositoryInputLayout.setError(errorMessage);
                    }
                });
            }
        }).execute();
    }
    
    private boolean isValidRepositoryFormat(String repository) {
        // Basic validation for address:port format
        return repository.contains(":") && repository.split(":").length == 2;
    }
    
    private interface ValidationCallback {
        void onValidationComplete(boolean isValid, String errorMessage);
    }
    
    private static class ValidateRepositoryTask extends AsyncTask<Void, Void, Boolean> {
        private final String address;
        private final int port;
        private final ValidationCallback callback;
        private String errorMessage = "Unable to connect to repository";
        
        public ValidateRepositoryTask(String address, int port, ValidationCallback callback) {
            this.address = address;
            this.port = port;
            this.callback = callback;
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // First check if the address is reachable (ping)
                InetAddress inetAddress = InetAddress.getByName(address);
                if (!inetAddress.isReachable(3000)) { // 3 second timeout
                    errorMessage = "Address is not reachable";
                    return false;
                }
                
                // Then check if the port is open
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(address, port), 3000); // 3 second timeout
                    return true;
                } catch (IOException e) {
                    errorMessage = "Port " + port + " is not open";
                    return false;
                }
            } catch (UnknownHostException e) {
                errorMessage = "Unknown host: " + address;
                return false;
            } catch (IOException e) {
                errorMessage = "Network error: " + e.getMessage();
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean isValid) {
            callback.onValidationComplete(isValid, errorMessage);
        }
    }
}
