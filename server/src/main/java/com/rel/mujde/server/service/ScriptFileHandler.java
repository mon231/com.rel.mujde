package com.rel.mujde.server.service;

import com.rel.mujde.server.db.DatabaseManager;
import com.rel.mujde.server.model.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles file operations for Frida scripts.
 */
public class ScriptFileHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScriptFileHandler.class);
    private static final String SCRIPTS_DIR = "stored_scripts";
    private final DatabaseManager dbManager;

    public ScriptFileHandler() {
        this.dbManager = DatabaseManager.getInstance();
        // Ensure scripts directory exists
        dbManager.getScriptsStorageDirectory();
    }

    /**
     * Generates a unique file path for a new script.
     * @return A unique file path
     */
    public String generateUniqueScriptPath() {
        String uniqueId = UUID.randomUUID().toString();
        return SCRIPTS_DIR + File.separator + uniqueId + ".js";
    }

    /**
     * Reads the content of a script from its file path.
     * @param scriptPath The file path of the script
     * @return The content of the script
     */
    public String readScriptContent(String scriptPath) {
        try {
            return new String(Files.readAllBytes(Paths.get(scriptPath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error reading script content from {}", scriptPath, e);
            throw new RuntimeException("Error reading script content", e);
        }
    }

    /**
     * Writes content to a script file.
     * @param scriptPath The file path to write to
     * @param content The content to write
     */
    public void writeScriptContent(String scriptPath, String content) {
        try {
            Files.write(Paths.get(scriptPath), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("Error writing script content to {}", scriptPath, e);
            throw new RuntimeException("Error writing script content", e);
        }
    }

    /**
     * Downloads a script from a URL and saves it to the local file system.
     * @param script The script to update
     * @return The updated script
     */
    public Script updateScriptFromUrl(Script script) {
        if (script.getNetworkPath() == null || script.getNetworkPath().isEmpty()) {
            logger.warn("No network path specified for script {}", script.getScriptName());
            return script;
        }

        try {
            URL url = new URL(script.getNetworkPath());
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }

                // Write the downloaded content to the script file
                writeScriptContent(script.getScriptPath(), content.toString());

                // Update the last modified timestamp
                script.setLastModified(LocalDateTime.now());
                dbManager.updateScript(script);

                logger.info("Successfully updated script {} from URL {}",
                        script.getScriptName(), script.getNetworkPath());
            }
        } catch (IOException e) {
            logger.error("Error downloading script from URL {}", script.getNetworkPath(), e);
            throw new RuntimeException("Error downloading script from URL", e);
        }

        return script;
    }

    /**
     * Deletes a script file from the file system.
     * @param scriptPath The path of the script file to delete
     * @return True if the file was deleted, false otherwise
     */
    public boolean deleteScriptFile(String scriptPath) {
        try {
            return Files.deleteIfExists(Paths.get(scriptPath));
        } catch (IOException e) {
            logger.error("Error deleting script file {}", scriptPath, e);
            throw new RuntimeException("Error deleting script file", e);
        }
    }
}