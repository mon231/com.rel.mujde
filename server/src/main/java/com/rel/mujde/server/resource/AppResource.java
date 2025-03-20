package com.rel.mujde.server.resource;

import com.rel.mujde.server.db.DatabaseManager;
import com.rel.mujde.server.model.App;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * RESTful resource for managing applications.
 */
@Path("/apps")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AppResource {
    private static final Logger logger = LoggerFactory.getLogger(AppResource.class);
    private final DatabaseManager dbManager;

    public AppResource() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Retrieves all registered applications.
     */
    @GET
    public Response getAllApps() {
        try {
            List<App> apps = dbManager.getAllApps();
            return Response.ok(apps).build();
        } catch (Exception e) {
            logger.error("Error retrieving all apps", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving applications: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Registers a new application.
     */
    @POST
    public Response addApp(App app) {
        try {
            if (app.getPackageName() == null || app.getPackageName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Package name is required")
                        .build();
            }

            App existingApp = dbManager.getAppByPackageName(app.getPackageName());
            if (existingApp != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("An application with package name '" + app.getPackageName() + "' already exists")
                        .build();
            }

            App addedApp = dbManager.addApp(app);
            return Response.status(Response.Status.CREATED)
                    .entity(addedApp)
                    .build();
        } catch (Exception e) {
            logger.error("Error adding app: {}", app, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error adding application: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieves details of a specific application by ID.
     */
    @GET
    @Path("/{id}")
    public Response getAppById(@PathParam("id") int appId) {
        try {
            App app = dbManager.getAppById(appId);
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Application with ID " + appId + " not found")
                        .build();
            }
            return Response.ok(app).build();
        } catch (Exception e) {
            logger.error("Error getting app by ID: {}", appId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving application: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Removes an application from the database.
     */
    @DELETE
    @Path("/{app_id}")
    public Response deleteApp(@PathParam("app_id") int appId) {
        try {
            App app = dbManager.getAppById(appId);
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Application with ID " + appId + " not found")
                        .build();
            }

            boolean deleted = dbManager.deleteApp(appId);
            if (deleted) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Failed to delete application")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error deleting app with ID: {}", appId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting application: " + e.getMessage())
                    .build();
        }
    }
}