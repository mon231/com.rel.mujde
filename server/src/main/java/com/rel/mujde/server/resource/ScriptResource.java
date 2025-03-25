package com.rel.mujde.server.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@Path("/scripts")
@Produces(MediaType.APPLICATION_JSON)
public class ScriptResource {
    private static final Logger logger = LoggerFactory.getLogger(ScriptResource.class);
    private static final ObjectMapper objectMapper;
    
    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Enable pretty printing for debugging
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    private final DatabaseManager dbManager;
    private final ScriptFileHandler scriptFileHandler;

    public ScriptResource() {
        this.dbManager = DatabaseManager.getInstance();
        this.scriptFileHandler = new ScriptFileHandler();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllScripts() {
        try {
            List<Script> scripts = dbManager.getAllScripts();
            // Convert to JSON string manually to avoid serialization issues
            String jsonScripts = objectMapper.writeValueAsString(scripts);
            return Response.ok(jsonScripts, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.error("Error retrieving all scripts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving scripts: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addScript(Script script) {
        try {
            if (script.getScriptName() == null || script.getScriptName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Script name can't be empty")
                        .build();
            }

            if (script.getContent() == null || script.getContent().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Script content can't be empty")
                        .build();
            }

            Script existingScript = dbManager.getScriptByName(script.getScriptName());

            if (existingScript != null) {
                scriptFileHandler.writeScriptContent(existingScript.getScriptPath(), script.getContent());
                existingScript.setLastModified(LocalDateTime.now());

                if (script.getNetworkPath() != null && !script.getNetworkPath().isEmpty()) {
                    existingScript.setNetworkPath(script.getNetworkPath());
                }

                dbManager.updateScript(existingScript);
                return Response.ok(existingScript).build();
            } else {
                String scriptPath = scriptFileHandler.generateUniqueScriptPath();

                script.setScriptPath(scriptPath);
                script.setLastModified(LocalDateTime.now());

                Script addedScript = dbManager.addScript(script);
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

            if (updatedScript.getScriptId() != scriptId){
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Script ID in URL path and request body don't match")
                        .build();
            }

            if (updatedScript.getScriptName() != null && !updatedScript.getScriptName().isEmpty()) {
                existingScript.setScriptName(updatedScript.getScriptName());
            }

            if (updatedScript.getNetworkPath() != null) {
                existingScript.setNetworkPath(updatedScript.getNetworkPath());
            }

            if (updatedScript.getContent() != null && !updatedScript.getContent().isEmpty()) {
                scriptFileHandler.writeScriptContent(existingScript.getScriptPath(), updatedScript.getContent());
            } else if (existingScript.getNetworkPath() != null) {
                // If content not provided but network path exists, update from URL
                scriptFileHandler.updateScriptFromUrl(existingScript);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid request: content or network path is required")
                .build();
            }

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

    @POST
    @Path("/update")
    public Response updateScripts() {
        try {
            List<Script> scripts = dbManager.getAllScripts();

            for (Script script : scripts) {
                if (script.getNetworkPath() == null) {
                    continue;
                }

                scriptFileHandler.updateScriptFromUrl(script);
                script.setLastModified(LocalDateTime.now());
                dbManager.updateScript(script);
            }

            return Response.ok().build();
        } catch (Exception e) {
            logger.error("Error updating scripts", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating scripts: " + e.getMessage())
                    .build();
        }
    }

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

            boolean fileDeleted = scriptFileHandler.deleteScriptFile(script.getScriptPath());
            if (!fileDeleted) {
                logger.warn("Script file {} not found or could not be deleted", script.getScriptPath());
            }

            boolean dbDeleted = dbManager.deleteScript(scriptId);
            if (dbDeleted) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                logger.warn("Script {} wasn't deleted from DB", script.getScriptName());
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
