package com.rel.mujde.server;

import com.rel.mujde.server.resource.AppResource;
import com.rel.mujde.server.resource.InjectionResource;
import com.rel.mujde.server.resource.ScriptResource;
import com.rel.mujde.server.serializer.JacksonConfig;

import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;

public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final String ALL_INTERFACES = "0.0.0.0";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static URI getServerBindings(String[] args) {
        int port = DEFAULT_PORT;
        String interfaceAddress = ALL_INTERFACES;

        for (int i = 0; i < args.length; i++) {
            if ("-p".equals(args[i]) || "--port".equals(args[i])) {
                if (i + 1 < args.length) {
                    port = Integer.parseInt(args[i + 1]);
                    i++;
                }
            } else if ("-i".equals(args[i]) || "--interface-address".equals(args[i])) {
                if (i + 1 < args.length) {
                    interfaceAddress = args[i + 1];
                    i++;
                }
            }
        }

        return UriBuilder.fromUri("http://" + interfaceAddress + "/").port(port).build();
    }

    public static void main(String[] args) {
        try {
            URI serverBindings = getServerBindings(args);
            ResourceConfig config = new ResourceConfig();

            config.register(JacksonConfig.class);
            config.register(AppResource.class);
            config.register(ScriptResource.class);
            config.register(InjectionResource.class);

            GrizzlyHttpServerFactory.createHttpServer(serverBindings, config);

            logger.info("Mujde Server started at {}", serverBindings);
            logger.info("Press Ctrl+C to stop the server...");

            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.info("Server interrupted");
        }
    }
}
