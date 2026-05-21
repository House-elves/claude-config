package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api/settings")
@Produces(MediaType.APPLICATION_JSON)
public class SettingsResource {

    @Inject
    ClaudeConfigService configService;

    @GET
    @jakarta.ws.rs.Path("/user")
    public Response getUserSettings() {
        return Response.ok(configService.readJsonFile(configService.userSettingsPath())).build();
    }

    @GET
    @jakarta.ws.rs.Path("/user-local")
    public Response getUserLocalSettings() {
        return Response.ok(configService.readJsonFile(configService.userLocalSettingsPath())).build();
    }

    @GET
    @jakarta.ws.rs.Path("/project")
    public Response getProjectSettings(@QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return Response.ok(configService.readJsonFile(configService.projectSettingsPath(path))).build();
    }

    @GET
    @jakarta.ws.rs.Path("/project-local")
    public Response getProjectLocalSettings(@QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return Response.ok(configService.readJsonFile(configService.projectLocalSettingsPath(path))).build();
    }

    @PUT
    @jakarta.ws.rs.Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putUserSettings(JsonNode content) {
        return writeSettings(configService.userSettingsPath(), content);
    }

    @PUT
    @jakarta.ws.rs.Path("/user-local")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putUserLocalSettings(JsonNode content) {
        return writeSettings(configService.userLocalSettingsPath(), content);
    }

    @PUT
    @jakarta.ws.rs.Path("/project")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putProjectSettings(@QueryParam("path") String path, JsonNode content) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return writeSettings(configService.projectSettingsPath(path), content);
    }

    @PUT
    @jakarta.ws.rs.Path("/project-local")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putProjectLocalSettings(@QueryParam("path") String path, JsonNode content) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return writeSettings(configService.projectLocalSettingsPath(path), content);
    }

    private Response writeSettings(Path settingsPath, JsonNode content) {
        try {
            configService.writeJsonFile(settingsPath, content);
            return Response.ok(Map.of("status", "saved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
