package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api/claude-md")
public class ClaudeMdResource {

    @Inject
    ClaudeConfigService configService;

    @GET
    @jakarta.ws.rs.Path("/user")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUserClaudeMd() {
        return Response.ok(configService.readTextFile(configService.userClaudeMdPath())).build();
    }

    @GET
    @jakarta.ws.rs.Path("/project")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getProjectClaudeMd(@QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity("path required").build();
        return Response.ok(configService.readTextFile(configService.projectClaudeMdPath(path))).build();
    }

    @PUT
    @jakarta.ws.rs.Path("/user")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putUserClaudeMd(String content) {
        return writeText(configService.userClaudeMdPath(), content);
    }

    @PUT
    @jakarta.ws.rs.Path("/project")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putProjectClaudeMd(@QueryParam("path") String path, String content) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return writeText(configService.projectClaudeMdPath(path), content);
    }

    private Response writeText(Path filePath, String content) {
        try {
            configService.writeTextFile(filePath, content);
            return Response.ok(Map.of("status", "saved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
