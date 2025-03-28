package com.rel.mujde.server.service;

import com.rel.mujde.server.model.Script;
import com.rel.mujde.server.db.DatabaseManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;

public class ScriptFileHandler {
    private final DatabaseManager dbManager;
    private static final String SCRIPTS_DIR = "stored_scripts";
    private static final Logger logger = LoggerFactory.getLogger(ScriptFileHandler.class);

    public ScriptFileHandler() {
        dbManager = DatabaseManager.getInstance();
        dbManager.getScriptsStorageDirectory();
    }

    public String generateUniqueScriptPath() {
        String uniqueId = UUID.randomUUID().toString();
        return SCRIPTS_DIR + File.separator + uniqueId + ".js";
    }

    public String readScriptContent(String scriptPath) {
        try {
            String fullPath;
            if (scriptPath.startsWith(SCRIPTS_DIR)) {
                fullPath = scriptPath;
            } else {
                fullPath = SCRIPTS_DIR + File.separator + scriptPath;
            }

            logger.debug("Reading script content from path: {}", fullPath);
            return new String(Files.readAllBytes(Paths.get(fullPath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Error reading script content from {}", scriptPath, e);
            throw new RuntimeException("Error reading script content", e);
        }
    }

    public void writeScriptContent(String scriptPath, String content) {
        try {
            String fullPath;
            if (scriptPath.startsWith(SCRIPTS_DIR)) {
                fullPath = scriptPath;
            } else {
                fullPath = SCRIPTS_DIR + File.separator + scriptPath;
            }

            logger.debug("Writing script content to path: {}", fullPath);
            Files.write(Paths.get(fullPath), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("Error writing script content to {}", scriptPath, e);
            throw new RuntimeException("Error writing script content", e);
        }
    }

    public void updateScriptFromUrl(Script script) {
        if (script.getNetworkPath() == null || script.getNetworkPath().isEmpty()) {
            logger.warn("No network path specified for script {}", script.getScriptName());
            return;
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

                writeScriptContent(script.getScriptPath(), content.toString());

                script.setLastModified(LocalDateTime.now());
                dbManager.updateScript(script);

                logger.info("Successfully updated script {} from URL {}",
                        script.getScriptName(), script.getNetworkPath());
            }
        } catch (IOException e) {
            logger.error("Error downloading script from URL {}", script.getNetworkPath(), e);
            throw new RuntimeException("Error downloading script from URL", e);
        }
    }

    public boolean deleteScriptFile(String scriptPath) {
        try {
            return Files.deleteIfExists(Paths.get(scriptPath));
        } catch (IOException e) {
            logger.error("Error deleting script file {}", scriptPath, e);
            throw new RuntimeException("Error deleting script file", e);
        }
    }
}
