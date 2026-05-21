package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.houseelves.config.PermissionAnalyzer.AnalyzedPermission;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api/permissions")
@Produces(MediaType.APPLICATION_JSON)
public class PermissionsResource {

    @Inject
    ClaudeConfigService configService;

    @Inject
    PermissionAnalyzer analyzer;

    @GET
    @jakarta.ws.rs.Path("/merged")
    public Response getMerged(@QueryParam("project") String project) {
        List<AnalyzedPermission> all = new ArrayList<>();

        all.addAll(analyzer.analyze(
                configService.getPermissions(configService.userSettingsPath()),
                "user", null));
        all.addAll(analyzer.analyze(
                configService.getPermissions(configService.userLocalSettingsPath()),
                "user-local", null));

        if (project != null && !project.isBlank()) {
            all.addAll(analyzer.analyze(
                    configService.getPermissions(configService.projectSettingsPath(project)),
                    "project", project));
            all.addAll(analyzer.analyze(
                    configService.getPermissions(configService.projectLocalSettingsPath(project)),
                    "project-local", project));
        }

        return Response.ok(all).build();
    }

    @GET
    @jakarta.ws.rs.Path("/stale")
    public Response getStale(@QueryParam("project") String project) {
        List<AnalyzedPermission> all = new ArrayList<>();

        all.addAll(analyzer.analyze(
                configService.getPermissions(configService.userSettingsPath()),
                "user", null));
        all.addAll(analyzer.analyze(
                configService.getPermissions(configService.userLocalSettingsPath()),
                "user-local", null));

        if (project != null && !project.isBlank()) {
            all.addAll(analyzer.analyze(
                    configService.getPermissions(configService.projectSettingsPath(project)),
                    "project", project));
            all.addAll(analyzer.analyze(
                    configService.getPermissions(configService.projectLocalSettingsPath(project)),
                    "project-local", project));
        }

        List<AnalyzedPermission> stale = all.stream().filter(AnalyzedPermission::stale).toList();

        Map<String, List<AnalyzedPermission>> grouped = new LinkedHashMap<>();
        for (AnalyzedPermission p : stale) {
            for (var reason : p.reasons()) {
                grouped.computeIfAbsent(reason.code(), k -> new ArrayList<>()).add(p);
            }
        }

        return Response.ok(Map.of("stale", stale, "grouped", grouped)).build();
    }

    @POST
    @jakarta.ws.rs.Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response add(Map<String, String> body) {
        String level = body.get("level");
        String project = body.get("projectPath");
        String pattern = body.get("pattern");

        if (pattern == null || pattern.isBlank()) {
            return Response.status(400).entity(Map.of("error", "pattern is required")).build();
        }

        Path settingsPath = resolveSettingsPath(level, project);
        if (settingsPath == null) {
            return Response.status(400).entity(Map.of("error", "invalid level")).build();
        }

        try {
            configService.addPermission(settingsPath, pattern);
            return Response.ok(Map.of("status", "added")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response remove(Map<String, String> body) {
        String level = body.get("level");
        String project = body.get("projectPath");
        String pattern = body.get("pattern");

        Path settingsPath = resolveSettingsPath(level, project);
        if (settingsPath == null) {
            return Response.status(400).entity(Map.of("error", "invalid level")).build();
        }

        try {
            boolean removed = configService.removePermission(settingsPath, pattern);
            return Response.ok(Map.of("status", removed ? "removed" : "not_found")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/move")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response move(Map<String, String> body) {
        String pattern = body.get("pattern");
        String fromLevel = body.get("fromLevel");
        String fromProject = body.get("fromProject");
        String toLevel = body.get("toLevel");
        String toProject = body.get("toProject");

        Path from = resolveSettingsPath(fromLevel, fromProject);
        Path to = resolveSettingsPath(toLevel, toProject);
        if (from == null || to == null) {
            return Response.status(400).entity(Map.of("error", "invalid level")).build();
        }

        try {
            configService.addPermission(to, pattern);
            configService.removePermission(from, pattern);
            return Response.ok(Map.of("status", "moved")).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @jakarta.ws.rs.Path("/bulk-remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bulkRemove(BulkRemoveRequest body) {
        Path settingsPath = resolveSettingsPath(body.level, body.projectPath);
        if (settingsPath == null) {
            return Response.status(400).entity(Map.of("error", "invalid level")).build();
        }

        int removed = 0;
        try {
            for (String pattern : body.patterns) {
                if (configService.removePermission(settingsPath, pattern)) {
                    removed++;
                }
            }
            return Response.ok(Map.of("status", "removed", "count", removed)).build();
        } catch (IOException e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    public static class BulkRemoveRequest {
        public String level;
        public String projectPath;
        public List<String> patterns;
    }

    private Path resolveSettingsPath(String level, String project) {
        if (level == null) return null;
        return switch (level) {
            case "user" -> configService.userSettingsPath();
            case "user-local" -> configService.userLocalSettingsPath();
            case "project" -> project != null ? configService.projectSettingsPath(project) : null;
            case "project-local" -> project != null ? configService.projectLocalSettingsPath(project) : null;
            default -> null;
        };
    }
}
