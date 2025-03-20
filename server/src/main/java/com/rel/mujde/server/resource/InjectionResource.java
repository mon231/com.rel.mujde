package com.rel.mujde.server.resource;

import com.rel.mujde.server.db.DatabaseManager;
import com.rel.mujde.server.model.App;
import com.rel.mujde.server.model.Injection;
import com.rel.mujde.server.model.Script;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RESTful resource for managing script injections into applications.
 */
@Path("/injections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InjectionResource {
    private static final Logger logger = LoggerFactory.getLogger(InjectionResource.class);
    private final DatabaseManager dbManager;

    public InjectionResource() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Gets all scripts to inject into a specific application.
     */
    @GET
    @Path("/by_app/{package_name}")
    public Response getInjectionsByApp(@PathParam("package_name") String packageName) {
        try {
            App app = dbManager.getAppByPackageName(packageName);
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Application with package name '" + packageName + "' not found")
                        .build();
            }

            List<Injection> injections = dbManager.getInjectionsByApp(packageName);
            return Response.ok(injections).build();
        } catch (Exception e) {
            logger.error("Error retrieving injections for app: {}", packageName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving injections: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Gets all applications that a specific script should be injected into.
     */
    @GET
    @Path("/by_script/{script_name}")
    public Response getInjectionsByScript(@PathParam("script_name") String scriptName) {
        try {
            Script script = dbManager.getScriptByName(scriptName);
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Script with name '" + scriptName + "' not found")
                        .build();
            }

            List<Injection> injections = dbManager.getInjectionsByScript(scriptName);
            return Response.ok(injections).build();
        } catch (Exception e) {
            logger.error("Error retrieving injections for script: {}", scriptName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving injections: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Associates a script with an application for injection.
     */
    @POST
    public Response addInjection(Injection injection) {
        try {
            if (injection.getPackageName() == null || injection.getPackageName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Package name is required")
                        .build();
            }

            if (injection.getScriptName() == null || injection.getScriptName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Script name is required")
                        .build();
            }

            // Get app and script by their names
            App app = dbManager.getAppByPackageName(injection.getPackageName());
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Application with package name '" + injection.getPackageName() + "' not found")
                        .build();
            }

            Script script = dbManager.getScriptByName(injection.getScriptName());
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Script with name '" + injection.getScriptName() + "' not found")
                        .build();
            }

            // Set IDs for the injection
            injection.setAppId(app.getAppId());
            injection.setScriptId(script.getScriptId());

            // Add the injection
            boolean added = dbManager.addInjection(injection);
            if (added) {
                return Response.status(Response.Status.CREATED)
                        .entity(injection)
                        .build();
            } else {
                return Response.status(Response.Status.CONFLICT)
                        .entity("This script is already associated with this application")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error adding injection: {}", injection, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error adding injection: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Removes an association between a script and an application.
     */
    @DELETE
    @Path("/{package_name}/{script_name}")
    public Response deleteInjection(
            @PathParam("package_name") String packageName,
            @PathParam("script_name") String scriptName) {
        try {
            App app = dbManager.getAppByPackageName(packageName);
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Application with package name '" + packageName + "' not found")
                        .build();
            }

            Script script = dbManager.getScriptByName(scriptName);
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Script with name '" + scriptName + "' not found")
                        .build();
            }

            boolean deleted = dbManager.deleteInjection(app.getAppId(), script.getScriptId());
            if (deleted) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No injection found for this application and script")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error deleting injection for app: {} and script: {}", packageName, scriptName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting injection: " + e.getMessage())
                    .build();
        }
    }
}