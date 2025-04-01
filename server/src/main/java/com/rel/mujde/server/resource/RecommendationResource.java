package com.rel.mujde.server.resource;

import com.rel.mujde.server.db.DatabaseManager;
import com.rel.mujde.server.model.App;
import com.rel.mujde.server.model.Recommendation;
import com.rel.mujde.server.model.Script;

import jakarta.ws.rs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/recommendations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecommendationResource {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationResource.class);
    private final DatabaseManager dbManager;

    public RecommendationResource() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @GET
    @Path("/by_app/{package_name}")
    public Response getRecommendationsByApp(@PathParam("package_name") String packageName) {
        try {
            App app = dbManager.getAppByPackageName(packageName);
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Application with package name '" + packageName + "' not found")
                    .build();
            }

            List<Recommendation> recommendations = dbManager.getRecommendationsByApp(packageName);
            return Response.ok(recommendations).build();
        } catch (Exception e) {
            logger.error("Error retrieving recommendations for app: {}", packageName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving recommendations: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/by_script/{script_name}")
    public Response getRecommendationsByScript(@PathParam("script_name") String scriptName) {
        try {
            Script script = dbManager.getScriptByName(scriptName);
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Script with name '" + scriptName + "' not found")
                    .build();
            }

            List<Recommendation> recommendations = dbManager.getRecommendationsByScript(scriptName);
            return Response.ok(recommendations).build();
        } catch (Exception e) {
            logger.error("Error retrieving recommendations for script: {}", scriptName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error retrieving recommendations: " + e.getMessage())
                .build();
        }
    }

    @POST
    public Response addRecommendation(Recommendation recommendation) {
        try {
            if (recommendation.getPackageName() == null || recommendation.getPackageName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Package name is required")
                    .build();
            }

            if (recommendation.getScriptName() == null || recommendation.getScriptName().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Script name is required")
                    .build();
            }

            App app = dbManager.getAppByPackageName(recommendation.getPackageName());
            if (app == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Application with package name '" + recommendation.getPackageName() + "' not found")
                    .build();
            }

            Script script = dbManager.getScriptByName(recommendation.getScriptName());
            if (script == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("Script with name '" + recommendation.getScriptName() + "' not found")
                    .build();
            }

            recommendation.setAppId(app.getAppId());
            recommendation.setScriptId(script.getScriptId());

            boolean added = dbManager.addRecommendation(recommendation);
            if (added) {
                return Response.status(Response.Status.CREATED)
                        .entity(recommendation)
                        .build();
            } else {
                return Response.status(Response.Status.CONFLICT)
                        .entity("This script is already associated with this application")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error adding recommendation: {}", recommendation, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error adding recommendation: " + e.getMessage())
                    .build();
        }
    }

    @DELETE
    @Path("/{package_name}/{script_name}")
    public Response deleteRecommendation(
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

            boolean deleted = dbManager.deleteRecommendation(app.getAppId(), script.getScriptId());
            if (deleted) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No recommendation found for this application and script")
                        .build();
            }
        } catch (Exception e) {
            logger.error("Error deleting recommendation for app: {} and script: {}", packageName, scriptName, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting recommendation: " + e.getMessage())
                    .build();
        }
    }
}
