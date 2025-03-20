package com.rel.mujde.server;

import com.rel.mujde.server.resource.AppResource;
import com.rel.mujde.server.resource.InjectionResource;
import com.rel.mujde.server.resource.ScriptResource;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Main class to start the Mujde server.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "0.0.0.0";

    public static void main(String[] args) {
        try {
            // Parse command line arguments
            int port = DEFAULT_PORT;
            String host = DEFAULT_HOST;

            for (int i = 0; i < args.length; i++) {
                if ("-p".equals(args[i]) || "--port".equals(args[i])) {
                    if (i + 1 < args.length) {
                        port = Integer.parseInt(args[i + 1]);
                        i++;
                    }
                } else if ("-h".equals(args[i]) || "--host".equals(args[i])) {
                    if (i + 1 < args.length) {
                        host = args[i + 1];
                        i++;
                    }
                }
            }

            // Create URI for server
            URI baseUri = UriBuilder.fromUri("http://" + host + "/").port(port).build();

            // Configure Jersey
            ResourceConfig config = new ResourceConfig();
            config.register(AppResource.class);
            config.register(ScriptResource.class);
            config.register(InjectionResource.class);

            // Create and start the server
            GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

            logger.info("Mujde Server started at {}:{}", host, port);
            logger.info("Press Ctrl+C to stop the server...");

            // Keep the main thread alive
            Thread.currentThread().join();

        } catch (IOException e) {
            logger.error("Error starting server", e);
            System.exit(1);
        } catch (InterruptedException e) {
            logger.info("Server interrupted");
        }
    }
}