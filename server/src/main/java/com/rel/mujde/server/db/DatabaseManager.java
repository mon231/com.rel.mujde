package com.rel.mujde.server.db;

import com.rel.mujde.server.model.App;
import com.rel.mujde.server.model.Script;
import com.rel.mujde.server.model.Recommendation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;

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
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
            Statement stmt = conn.createStatement()) {

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Apps (" +
                    "app_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "package_name TEXT UNIQUE NOT NULL" +
                ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Scripts (" +
                    "script_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "script_name TEXT UNIQUE NOT NULL," +
                    "script_path TEXT NOT NULL," +
                    "network_path TEXT," +
                    "last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Recommendation (" +
                    "app_id INTEGER," +
                    "script_id INTEGER," +
                    "PRIMARY KEY (app_id, script_id)," +
                    "FOREIGN KEY (app_id) REFERENCES Apps(app_id) ON DELETE CASCADE," +
                    "FOREIGN KEY (script_id) REFERENCES Scripts(script_id) ON DELETE CASCADE" +
                ");"
            );

            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
            throw new RuntimeException("Error initializing database", e);
        }
    }

    public File getScriptsStorageDirectory() {
        File dir = new File("stored_scripts");

        if (!dir.exists()) {
            if (dir.mkdir()) {
                logger.info("Created scripts storage directory: {}", dir.getAbsolutePath());
            } else {
                logger.error("Failed to create scripts storage directory: {}", dir.getAbsolutePath());
                throw new RuntimeException("Failed to create scripts storage directory");
            }
        } else if (!dir.isDirectory()) {
            logger.error("Non-directory exists where storage-directory is set: {}", dir.getAbsolutePath());
            throw new RuntimeException("Can't create scripts storage directory");
        }

        return dir;
    }

    public List<App> getAllApps() {
        List<App> apps = new ArrayList<>();
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public App getAppById(int appId) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public App getAppByPackageName(String packageName) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public App addApp(App app) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public boolean deleteApp(int appId) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM Apps WHERE app_id = ?")) {

            stmt.setInt(1, appId);
            int affectedRows = stmt.executeUpdate();

            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting app with ID: {}", appId, e);
            throw new RuntimeException("Error deleting app", e);
        }
    }

    public List<Script> getAllScripts() {
        List<Script> scripts = new ArrayList<>();
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public Script getScriptByName(String scriptName) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public Script addScript(Script script) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public boolean updateScript(Script script) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
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

    public boolean deleteScript(int scriptId) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM Scripts WHERE script_id = ?")) {
            stmt.setInt(1, scriptId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting script with ID: {}", scriptId, e);
            throw new RuntimeException("Error deleting script", e);
        }
    }

    public List<Recommendation> getAllRecommendations() {
        List<Recommendation> recommendations = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT i.app_id, i.script_id, a.package_name, s.script_name " +
                     "FROM Recommendation i " +
                     "JOIN Apps a ON i.app_id = a.app_id " +
                     "JOIN Scripts s ON i.script_id = s.script_id")) {

            while (rs.next()) {
                Recommendation recommendation = new Recommendation(rs.getInt("app_id"), rs.getInt("script_id"));
                recommendation.setPackageName(rs.getString("package_name"));
                recommendation.setScriptName(rs.getString("script_name"));
                recommendations.add(recommendation);
            }
        } catch (SQLException e) {
            logger.error("Error getting all recommendations", e);
            throw new RuntimeException("Error getting all recommendations", e);
        }

        return recommendations;
    }

    public List<Recommendation> getRecommendationsByApp(String packageName) {
        List<Recommendation> recommendations = new ArrayList<>();

        try (
            Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement(
                     "SELECT i.app_id, i.script_id, a.package_name, s.script_name " +
                     "FROM Recommendations i " +
                     "JOIN Apps a ON i.app_id = a.app_id " +
                     "JOIN Scripts s ON i.script_id = s.script_id " +
                     "WHERE a.package_name = ?")) {

            stmt.setString(1, packageName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Recommendation recommendation = new Recommendation(rs.getInt("app_id"), rs.getInt("script_id"));
                    recommendation.setPackageName(rs.getString("package_name"));
                    recommendation.setScriptName(rs.getString("script_name"));
                    recommendations.add(recommendation);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting recommendations by app: {}", packageName, e);
            throw new RuntimeException("Error getting recommendations by app", e);
        }

        return recommendations;
    }

    public List<Recommendation> getRecommendationsByScript(String scriptName) {
        List<Recommendation> recommendations = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT i.app_id, i.script_id, a.package_name, s.script_name " +
                     "FROM Recommendations i " +
                     "JOIN Apps a ON i.app_id = a.app_id " +
                     "JOIN Scripts s ON i.script_id = s.script_id " +
                     "WHERE s.script_name = ?")) {

            stmt.setString(1, scriptName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Recommendation recommendation = new Recommendation(rs.getInt("app_id"), rs.getInt("script_id"));
                    recommendation.setPackageName(rs.getString("package_name"));
                    recommendation.setScriptName(rs.getString("script_name"));
                    recommendations.add(recommendation);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting recommendations by script: {}", scriptName, e);
            throw new RuntimeException("Error getting recommendations by script", e);
        }

        return recommendations;
    }

    public boolean addRecommendation(Recommendation recommendation) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO Recommendation (app_id, script_id) VALUES (?, ?)")) {

            stmt.setInt(1, recommendation.getAppId());
            stmt.setInt(2, recommendation.getScriptId());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error adding recommendation: {}", recommendation, e);
            throw new RuntimeException("Error adding recommendation", e);
        }
    }

    public boolean deleteRecommendation(int appId, int scriptId) {
        try (
            Connection conn = DriverManager.getConnection(DB_URL);
            PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM Recommendations WHERE app_id = ? AND script_id = ?")) {

            stmt.setInt(1, appId);
            stmt.setInt(2, scriptId);

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            logger.error("Error deleting recommendation for app ID: {} and script ID: {}", appId, scriptId, e);
            throw new RuntimeException("Error deleting recommendation", e);
        }
    }
}
