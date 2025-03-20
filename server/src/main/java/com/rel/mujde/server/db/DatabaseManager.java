package com.rel.mujde.server.db;

import com.rel.mujde.server.model.App;
import com.rel.mujde.server.model.Injection;
import com.rel.mujde.server.model.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages interactions with the SQLite database.
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:mujde.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initialize the database by creating tables if they don't exist.
     */
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Create Apps table
            stmt.execute("CREATE TABLE IF NOT EXISTS Apps (" +
                    "app_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "package_name TEXT UNIQUE NOT NULL" +
                    ");");

            // Create Scripts table
            stmt.execute("CREATE TABLE IF NOT EXISTS Scripts (" +
                    "script_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "script_name TEXT UNIQUE NOT NULL," +
                    "script_path TEXT NOT NULL," +
                    "network_path TEXT," +
                    "last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // Create Injections table
            stmt.execute("CREATE TABLE IF NOT EXISTS Injections (" +
                    "app_id INTEGER," +
                    "script_id INTEGER," +
                    "PRIMARY KEY (app_id, script_id)," +
                    "FOREIGN KEY (app_id) REFERENCES Apps(app_id) ON DELETE CASCADE," +
                    "FOREIGN KEY (script_id) REFERENCES Scripts(script_id) ON DELETE CASCADE" +
                    ");");

            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException("Error initializing database", e);
        }
    }

    /**
     * Create the scripts storage directory if it doesn't exist.
     * @return The scripts storage directory
     */
    public File getScriptsStorageDirectory() {
        File dir = new File("stored_scripts");
        if (!dir.exists()) {
            if (dir.mkdir()) {
                logger.info("Created scripts storage directory: {}", dir.getAbsolutePath());
            } else {
                logger.error("Failed to create scripts storage directory: {}", dir.getAbsolutePath());
                throw new RuntimeException("Failed to create scripts storage directory");
            }
        }
        return dir;
    }

    // App-related operations

    /**
     * Get all applications from the database.
     * @return List of all applications
     */
    public List<App> getAllApps() {
        List<App> apps = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT app_id, package_name FROM Apps")) {

            while (rs.next()) {
                App app = new App(rs.getInt("app_id"), rs.getString("package_name"));
                apps.add(app);
            }
        } catch (SQLException e) {
            logger.error("Error getting all apps", e);
            throw new RuntimeException("Error getting all apps", e);
        }
        return apps;
    }

    /**
     * Get an application by its ID.
     * @param appId The application ID
     * @return The application, or null if not found
     */
    public App getAppById(int appId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT app_id, package_name FROM Apps WHERE app_id = ?")) {

            stmt.setInt(1, appId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new App(rs.getInt("app_id"), rs.getString("package_name"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting app by ID: {}", appId, e);
            throw new RuntimeException("Error getting app by ID", e);
        }
        return null;
    }

    /**
     * Get an application by its package name.
     * @param packageName The package name
     * @return The application, or null if not found
     */
    public App getAppByPackageName(String packageName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("SELECT app_id, package_name FROM Apps WHERE package_name = ?")) {

            stmt.setString(1, packageName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new App(rs.getInt("app_id"), rs.getString("package_name"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting app by package name: {}", packageName, e);
            throw new RuntimeException("Error getting app by package name", e);
        }
        return null;
    }

    /**
     * Add a new application to the database.
     * @param app The application to add
     * @return The added application with its assigned ID
     */
    public App addApp(App app) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Apps (package_name) VALUES (?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, app.getPackageName());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating app failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    app.setAppId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating app failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding app: {}", app, e);
            throw new RuntimeException("Error adding app", e);
        }
        return app;
    }

    /**
     * Delete an application from the database.
     * @param appId The ID of the application to delete
     * @return True if the application was deleted, false otherwise
     */
    public boolean deleteApp(int appId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Apps WHERE app_id = ?")) {

            stmt.setInt(1, appId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting app with ID: {}", appId, e);
            throw new RuntimeException("Error deleting app", e);
        }
    }

    // Script-related operations

    /**
     * Get all scripts from the database.
     * @return List of all scripts
     */
    public List<Script> getAllScripts() {
        List<Script> scripts = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT script_id, script_name, script_path, network_path, last_modified FROM Scripts")) {

            while (rs.next()) {
                Script script = new Script(
                        rs.getInt("script_id"),
                        rs.getString("script_name"),
                        rs.getString("script_path"),
                        rs.getString("network_path"),
                        rs.getTimestamp("last_modified").toLocalDateTime()
                );
                scripts.add(script);
            }
        } catch (SQLException e) {
            logger.error("Error getting all scripts", e);
            throw new RuntimeException("Error getting all scripts", e);
        }
        return scripts;
    }

    /**
     * Get a script by its ID.
     * @param scriptId The script ID
     * @return The script, or null if not found
     */
    public Script getScriptById(int scriptId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT script_id, script_name, script_path, network_path, last_modified FROM Scripts WHERE script_id = ?")) {

            stmt.setInt(1, scriptId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Script(
                            rs.getInt("script_id"),
                            rs.getString("script_name"),
                            rs.getString("script_path"),
                            rs.getString("network_path"),
                            rs.getTimestamp("last_modified").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting script by ID: {}", scriptId, e);
            throw new RuntimeException("Error getting script by ID", e);
        }
        return null;
    }

    /**
     * Get a script by its name.
     * @param scriptName The script name
     * @return The script, or null if not found
     */
    public Script getScriptByName(String scriptName) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT script_id, script_name, script_path, network_path, last_modified FROM Scripts WHERE script_name = ?")) {

            stmt.setString(1, scriptName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Script(
                            rs.getInt("script_id"),
                            rs.getString("script_name"),
                            rs.getString("script_path"),
                            rs.getString("network_path"),
                            rs.getTimestamp("last_modified").toLocalDateTime()
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting script by name: {}", scriptName, e);
            throw new RuntimeException("Error getting script by name", e);
        }
        return null;
    }

    /**
     * Add a new script to the database.
     * @param script The script to add
     * @return The added script with its assigned ID
     */
    public Script addScript(Script script) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Scripts (script_name, script_path, network_path, last_modified) VALUES (?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, script.getScriptName());
            stmt.setString(2, script.getScriptPath());
            stmt.setString(3, script.getNetworkPath());
            stmt.setTimestamp(4, Timestamp.valueOf(script.getLastModified() != null ?
                                               script.getLastModified() : LocalDateTime.now()));
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating script failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    script.setScriptId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating script failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            logger.error("Error adding script: {}", script, e);
            throw new RuntimeException("Error adding script", e);
        }
        return script;
    }

    /**
     * Update an existing script.
     * @param script The script to update
     * @return True if the script was updated, false otherwise
     */
    public boolean updateScript(Script script) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Scripts SET script_name = ?, script_path = ?, network_path = ?, last_modified = ? WHERE script_id = ?")) {

            stmt.setString(1, script.getScriptName());
            stmt.setString(2, script.getScriptPath());
            stmt.setString(3, script.getNetworkPath());
            stmt.setTimestamp(4, Timestamp.valueOf(script.getLastModified() != null ?
                                               script.getLastModified() : LocalDateTime.now()));
            stmt.setInt(5, script.getScriptId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error updating script: {}", script, e);
            throw new RuntimeException("Error updating script", e);
        }
    }

    /**
     * Delete a script from the database.
     * @param scriptId The ID of the script to delete
     * @return True if the script was deleted, false otherwise
     */
    public boolean deleteScript(int scriptId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Scripts WHERE script_id = ?")) {

            stmt.setInt(1, scriptId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting script with ID: {}", scriptId, e);
            throw new RuntimeException("Error deleting script", e);
        }
    }

    // Injection-related operations

    /**
     * Get all injections from the database.
     * @return List of all injections
     */
    public List<Injection> getAllInjections() {
        List<Injection> injections = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT i.app_id, i.script_id, a.package_name, s.script_name " +
                     "FROM Injections i " +
                     "JOIN Apps a ON i.app_id = a.app_id " +
                     "JOIN Scripts s ON i.script_id = s.script_id")) {

            while (rs.next()) {
                Injection injection = new Injection(rs.getInt("app_id"), rs.getInt("script_id"));
                injection.setPackageName(rs.getString("package_name"));
                injection.setScriptName(rs.getString("script_name"));
                injections.add(injection);
            }
        } catch (SQLException e) {
            logger.error("Error getting all injections", e);
            throw new RuntimeException("Error getting all injections", e);
        }
        return injections;
    }

    /**
     * Get all injections for a specific application.
     * @param packageName The package name of the application
     * @return List of injections for the application
     */
    public List<Injection> getInjectionsByApp(String packageName) {
        List<Injection> injections = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT i.app_id, i.script_id, a.package_name, s.script_name " +
                     "FROM Injections i " +
                     "JOIN Apps a ON i.app_id = a.app_id " +
                     "JOIN Scripts s ON i.script_id = s.script_id " +
                     "WHERE a.package_name = ?")) {

            stmt.setString(1, packageName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Injection injection = new Injection(rs.getInt("app_id"), rs.getInt("script_id"));
                    injection.setPackageName(rs.getString("package_name"));
                    injection.setScriptName(rs.getString("script_name"));
                    injections.add(injection);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting injections by app: {}", packageName, e);
            throw new RuntimeException("Error getting injections by app", e);
        }
        return injections;
    }

    /**
     * Get all injections for a specific script.
     * @param scriptName The name of the script
     * @return List of injections for the script
     */
    public List<Injection> getInjectionsByScript(String scriptName) {
        List<Injection> injections = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT i.app_id, i.script_id, a.package_name, s.script_name " +
                     "FROM Injections i " +
                     "JOIN Apps a ON i.app_id = a.app_id " +
                     "JOIN Scripts s ON i.script_id = s.script_id " +
                     "WHERE s.script_name = ?")) {

            stmt.setString(1, scriptName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Injection injection = new Injection(rs.getInt("app_id"), rs.getInt("script_id"));
                    injection.setPackageName(rs.getString("package_name"));
                    injection.setScriptName(rs.getString("script_name"));
                    injections.add(injection);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting injections by script: {}", scriptName, e);
            throw new RuntimeException("Error getting injections by script", e);
        }
        return injections;
    }

    /**
     * Add a new injection to the database.
     * @param injection The injection to add
     * @return True if the injection was added, false otherwise
     */
    public boolean addInjection(Injection injection) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Injections (app_id, script_id) VALUES (?, ?)")) {

            stmt.setInt(1, injection.getAppId());
            stmt.setInt(2, injection.getScriptId());
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error adding injection: {}", injection, e);
            throw new RuntimeException("Error adding injection", e);
        }
    }

    /**
     * Delete an injection from the database.
     * @param appId The ID of the application
     * @param scriptId The ID of the script
     * @return True if the injection was deleted, false otherwise
     */
    public boolean deleteInjection(int appId, int scriptId) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM Injections WHERE app_id = ? AND script_id = ?")) {

            stmt.setInt(1, appId);
            stmt.setInt(2, scriptId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting injection for app ID: {} and script ID: {}", appId, scriptId, e);
            throw new RuntimeException("Error deleting injection", e);
        }
    }
}