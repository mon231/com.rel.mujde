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
    private SharedPreferences pref;
    private TextInputEditText repositoryInput;
    private TextInputLayout repositoryInputLayout;
    private Button saveRepositoryButton;
    private boolean isCurrentlyValidated = false;

    public HomeFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        pref = requireActivity().getSharedPreferences(Constants.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);

        // Initialize views
        repositoryInputLayout = view.findViewById(R.id.repository_input_layout);
        repositoryInput = view.findViewById(R.id.repository_input);
        saveRepositoryButton = view.findViewById(R.id.save_repository_button);

        // Load saved repository URL or set default
        String savedRepository = pref.getString(Constants.PREF_SCRIPTS_REPOSITORY, Constants.DEFAULT_REPOSITORY);
        repositoryInput.setText(savedRepository);

        saveRepositoryButton.setOnClickListener(e -> saveRepositoryUrl());
        return view;
    }

    private void saveRepositoryUrl() {
        if (isCurrentlyValidated) {
            return;
        }

        repositoryInputLayout.setError(null);
        String repositoryUrl = repositoryInput.getText().toString().trim();
        String[] urlParts = repositoryUrl.split(":");

        final int URL_PARTS_COUNT = 2;
        if (urlParts.length != URL_PARTS_COUNT) {
            repositoryInputLayout.setError("Unsupported URL format. Please use address:port format");
            return;
        }

        int port = 0;
        String address = urlParts[0];

        if (address.isEmpty()) {
            repositoryInputLayout.setError("Invalid empty server address");
            return;
        }

        try {
            port = Integer.parseInt(urlParts[1]);
        } catch (NumberFormatException e) {
            repositoryInputLayout.setError("Invalid non-integer port number");
            return;
        }

        if (port <= 0 || port > 65535) {
            repositoryInputLayout.setError("Port must be between 1 and 65535");
            return;
        }

        // now start a long-duration connectivity validation
        saveRepositoryButton.setEnabled(false);
        saveRepositoryButton.setText("Validating...");
        isCurrentlyValidated = true;

        new ValidateRepositoryTask(address, port, new ValidationCallback() {
            @Override
            public void onValidationComplete(boolean isValid, String errorMessage) {
                requireActivity().runOnUiThread(() -> {
                    saveRepositoryButton.setEnabled(true);
                    saveRepositoryButton.setText("Save");
                    isCurrentlyValidated = false;

                    if (!isValid) {
                        repositoryInputLayout.setError(errorMessage);
                        return;
                    }

                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(Constants.PREF_SCRIPTS_REPOSITORY, repositoryUrl);

                    editor.apply();
                    Toast.makeText(requireContext(), "Repository URL saved", Toast.LENGTH_SHORT).show();
                });
            }
        }).execute();
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
                // ping to server address inorder to ensure reachability
                InetAddress inetAddress = InetAddress.getByName(address);

                final int PING_TIMEOUT_MS = 3000;
                if (!inetAddress.isReachable(PING_TIMEOUT_MS)) {
                    errorMessage = "Address is not reachable";
                    return false;
                }

                // connect to server port via tcp to ensure reachability
                try (Socket socket = new Socket()) {
                    final int TCP_CONNECT_TIMEOUT_MS = 3000;
                    socket.connect(new InetSocketAddress(address, port), TCP_CONNECT_TIMEOUT_MS);
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
