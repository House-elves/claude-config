package io.quarkus.houseelves.config;

import java.io.IOException;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class AppConfigResource {

    @Inject
    ClaudeConfigService configService;

    @Inject
    ProjectDiscoveryService discoveryService;

    @GET
    @jakarta.ws.rs.Path("/app-config")
    public Response getAppConfig() {
        return Response.ok(configService.getAppConfig()).build();
    }

    @PUT
    @jakarta.ws.rs.Path("/app-config")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAppConfig(Map<String, String> updates) {
        try {
            configService.updateAppConfig(updates);
            return Response.ok(Map.of("status", "saved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @jakarta.ws.rs.Path("/projects")
    public Response getProjects() {
        return Response.ok(discoveryService.discoverAll()).build();
    }

    @POST
    @jakarta.ws.rs.Path("/projects/refresh")
    public Response refreshProjects() {
        return Response.ok(discoveryService.discoverAll()).build();
    }
}
