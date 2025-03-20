package com.rel.mujde.server.resource;

import com.rel.mujde.server.db.DatabaseManager;
import com.rel.mujde.server.model.Script;
import com.rel.mujde.server.service.ScriptFileHandler;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RESTful resource for managing Frida scripts.
 */
@Path("/scripts")
@Produces(MediaType.APPLICATION_JSON)
public class ScriptResource {
    private static final Logger logger = LoggerFactory.getLogger(ScriptResource.class);
    private final DatabaseManager dbManager;
    private final ScriptFileHandler scriptFileHandler;

    public ScriptResource() {
        this.dbManager = DatabaseManager.getInstance();
        this.scriptFileHandler = new ScriptFileHandler();
    }

    /**
     * Retrieves all available scripts.
     */
    @GET
    public Response getAllScripts() {
        try {
            List<Script> scripts = dbManager.getAllScripts();
            return Response.ok(scripts).build();
        } catch (Exception e) {
            logger.error("Error retrieving all scripts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving scripts: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Uploads a new script or updates an existing one.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addScript(Script script) {
        try {
            if (script.getScriptName() == null || script.getScriptName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Script name is required")
                        .build();
            }

            if (script.getContent() == null || script.getContent().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Script content is required")
                        .build();
            }

            // Check if script already exists
            Script existingScript = dbManager.getScriptByName(script.getScriptName());
            if (existingScript != null) {
                // Update existing script
                scriptFileHandler.writeScriptContent(existingScript.getScriptPath(), script.getContent());
                existingScript.setLastModified(LocalDateTime.now());
                if (script.getNetworkPath() != null && !script.getNetworkPath().isEmpty()) {
                    existingScript.setNetworkPath(script.getNetworkPath());
                }
                dbManager.updateScript(existingScript);

                return Response.ok(existingScript).build();
            } else {
                // Create new script
                String scriptPath = scriptFileHandler.generateUniqueScriptPath();
                script.setScriptPath(scriptPath);
                script.setLastModified(LocalDateTime.now());

                // Save script to database
                Script addedScript = dbManager.addScript(script);

                // Write script content to file
                scriptFileHandler.writeScriptContent(scriptPath, script.getContent());

                return Response.status(Response.Status.CREATED)
                        .entity(addedScript)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error adding script: {}", script.getScriptName(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error adding script: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieves a specific script by ID.
     */
    @GET
    @Path("/{script_id}")
    public Response getScriptById(@PathParam("script_id") int scriptId) {
        try {
            Script script = dbManager.getScriptById(scriptId);
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Script with ID " + scriptId + " not found")
                        .build();
            }

            // Read script content
            String content = scriptFileHandler.readScriptContent(script.getScriptPath());
            script.setContent(content);

            return Response.ok(script).build();
        } catch (Exception e) {
            logger.error("Error getting script by ID: {}", scriptId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving script: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Updates the content of an existing script.
     */
    @PUT
    @Path("/{script_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateScript(@PathParam("script_id") int scriptId, Script updatedScript) {
        try {
            Script existingScript = dbManager.getScriptById(scriptId);
            if (existingScript == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Script with ID " + scriptId + " not found")
                        .build();
            }

            // Update script properties if provided
            if (updatedScript.getScriptName() != null && !updatedScript.getScriptName().isEmpty()) {
                existingScript.setScriptName(updatedScript.getScriptName());
            }

            if (updatedScript.getNetworkPath() != null) {
                existingScript.setNetworkPath(updatedScript.getNetworkPath());
            }

            // Update script content if provided
            if (updatedScript.getContent() != null && !updatedScript.getContent().isEmpty()) {
                scriptFileHandler.writeScriptContent(existingScript.getScriptPath(), updatedScript.getContent());
            } else if (existingScript.getNetworkPath() != null && !existingScript.getNetworkPath().isEmpty()) {
                // If content not provided but network path exists, update from URL
                scriptFileHandler.updateScriptFromUrl(existingScript);
            }

            // Update last modified timestamp
            existingScript.setLastModified(LocalDateTime.now());
            dbManager.updateScript(existingScript);

            return Response.ok(existingScript).build();
        } catch (Exception e) {
            logger.error("Error updating script with ID: {}", scriptId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating script: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Removes a script.
     */
    @DELETE
    @Path("/{script_id}")
    public Response deleteScript(@PathParam("script_id") int scriptId) {
        try {
            Script script = dbManager.getScriptById(scriptId);
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Script with ID " + scriptId + " not found")
                        .build();
            }

            // Delete script file
            boolean fileDeleted = scriptFileHandler.deleteScriptFile(script.getScriptPath());
            if (!fileDeleted) {
                logger.warn("Script file {} not found or could not be deleted", script.getScriptPath());
            }

            // Delete script from database
            boolean dbDeleted = dbManager.deleteScript(scriptId);
            if (dbDeleted) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Failed to delete script from database")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error deleting script with ID: {}", scriptId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting script: " + e.getMessage())
                    .build();
        }
    }
}