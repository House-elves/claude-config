package io.quarkus.houseelves.config;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api/keybindings")
@Produces(MediaType.APPLICATION_JSON)
public class KeybindingsResource {

    @Inject
    ClaudeConfigService configService;

    @GET
    public Response getKeybindings() {
        return Response.ok(configService.readJsonFile(configService.keybindingsPath())).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putKeybindings(JsonNode content) {
        try {
            configService.writeJsonFile(configService.keybindingsPath(), content);
            return Response.ok(Map.of("status", "saved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
