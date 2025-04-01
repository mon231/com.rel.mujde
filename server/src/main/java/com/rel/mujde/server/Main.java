package com.rel.mujde.server;

import com.rel.mujde.server.resource.AppResource;
import com.rel.mujde.server.resource.RecommendationResource;
import com.rel.mujde.server.resource.ScriptResource;
import com.rel.mujde.server.serializer.JacksonConfig;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.core.UriBuilder;

public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String ALL_INTERFACES = "0.0.0.0";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static URI getServerBindings(String[] args) {
        int port = DEFAULT_PORT;
        String interfaceAddress = ALL_INTERFACES;

        boolean portSpecified = false;
        boolean hostSpecified = false;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && !portSpecified) {
                if (i + 1 < args.length) {
                    port = Integer.parseInt(args[i + 1]);
                    portSpecified = true;
                    i++;
                }
            } else if ("--host".equals(args[i]) && !hostSpecified) {
                if (i + 1 < args.length) {
                    interfaceAddress = args[i + 1];
                    hostSpecified = true;
                    i++;
                }
            }
        }

        return UriBuilder.fromUri("http://" + interfaceAddress + "/").port(port).build();
    }

    public static void main(String[] args) {
        try {
            ResourceConfig config = new ResourceConfig();

            // NOTE register Jackson for JSON serialization
            config.register(JacksonConfig.class);
            config.register(JacksonFeature.class);
            config.register(JacksonJaxbJsonProvider.class);
            config.packages("com.rel.mujde.server");

            // NOTE register resource classes for the server
            config.register(AppResource.class);
            config.register(ScriptResource.class);
            config.register(RecommendationResource.class);

            URI serverBindings = getServerBindings(args);
            GrizzlyHttpServerFactory.createHttpServer(serverBindings, config);

            logger.info("Mujde Server started at {}", serverBindings);
            logger.info("Press Ctrl+C to stop the server...");

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("Server interrupted");
        }
    }
}
