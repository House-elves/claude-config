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

@jakarta.ws.rs.Path("/api/mcp")
@Produces(MediaType.APPLICATION_JSON)
public class McpConfigResource {

    @Inject
    ClaudeConfigService configService;

    @GET
    @jakarta.ws.rs.Path("/user")
    public Response getUserMcp() {
        return Response.ok(configService.readJsonFile(configService.userMcpPath())).build();
    }

    @GET
    @jakarta.ws.rs.Path("/project")
    public Response getProjectMcp(@QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return Response.ok(configService.readJsonFile(configService.projectMcpPath(path))).build();
    }

    @PUT
    @jakarta.ws.rs.Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putUserMcp(JsonNode content) {
        return writeJson(configService.userMcpPath(), content);
    }

    @PUT
    @jakarta.ws.rs.Path("/project")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putProjectMcp(@QueryParam("path") String path, JsonNode content) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return writeJson(configService.projectMcpPath(path), content);
    }

    private Response writeJson(Path filePath, JsonNode content) {
        try {
            configService.writeJsonFile(filePath, content);
            return Response.ok(Map.of("status", "saved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
