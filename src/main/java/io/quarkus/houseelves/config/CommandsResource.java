package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api/commands")
@Produces(MediaType.APPLICATION_JSON)
public class CommandsResource {

    @Inject
    ClaudeConfigService configService;

    @GET
    @jakarta.ws.rs.Path("/user")
    public Response getUserCommands() {
        return Response.ok(configService.readCommands(configService.userCommandsDir())).build();
    }

    @GET
    @jakarta.ws.rs.Path("/project")
    public Response getProjectCommands(@QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        return Response.ok(configService.readCommands(configService.projectCommandsDir(path))).build();
    }

    @GET
    @jakarta.ws.rs.Path("/user/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUserCommand(@PathParam("name") String name) {
        Path file = configService.userCommandsDir().resolve(name + ".md");
        return Response.ok(configService.readTextFile(file)).build();
    }

    @GET
    @jakarta.ws.rs.Path("/project/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getProjectCommand(@PathParam("name") String name, @QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity("path required").build();
        Path file = configService.projectCommandsDir(path).resolve(name + ".md");
        return Response.ok(configService.readTextFile(file)).build();
    }

    @PUT
    @jakarta.ws.rs.Path("/user/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response putUserCommand(@PathParam("name") String name, String content) {
        Path file = configService.userCommandsDir().resolve(name + ".md");
        return writeCommand(file, content);
    }

    @PUT
    @jakarta.ws.rs.Path("/project/{name}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response putProjectCommand(@PathParam("name") String name, @QueryParam("path") String path, String content) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        Path file = configService.projectCommandsDir(path).resolve(name + ".md");
        return writeCommand(file, content);
    }

    @DELETE
    @jakarta.ws.rs.Path("/user/{name}")
    public Response deleteUserCommand(@PathParam("name") String name) {
        Path file = configService.userCommandsDir().resolve(name + ".md");
        return deleteCommand(file);
    }

    @DELETE
    @jakarta.ws.rs.Path("/project/{name}")
    public Response deleteProjectCommand(@PathParam("name") String name, @QueryParam("path") String path) {
        if (path == null) return Response.status(400).entity(Map.of("error", "path required")).build();
        Path file = configService.projectCommandsDir(path).resolve(name + ".md");
        return deleteCommand(file);
    }

    private Response writeCommand(Path file, String content) {
        try {
            configService.writeTextFile(file, content);
            return Response.ok(Map.of("status", "saved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    private Response deleteCommand(Path file) {
        try {
            if (Files.exists(file)) {
                Files.delete(file);
                return Response.ok(Map.of("status", "deleted")).build();
            }
            return Response.status(404).entity(Map.of("error", "not found")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
