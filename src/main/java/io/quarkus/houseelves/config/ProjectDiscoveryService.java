package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ProjectDiscoveryService {

    public record DiscoveredProject(
            String path,
            String name,
            boolean hasClaudeDir,
            boolean hasClaudeMd,
            boolean hasMcpJson,
            boolean hasProjectSettings,
            boolean hasProjectLocalSettings,
            boolean hasCommands,
            int permissionCount) {}

    @Inject
    ClaudeConfigService configService;

    public List<DiscoveredProject> discoverAll() {
        List<DiscoveredProject> projects = new ArrayList<>();
        String pathsStr = configService.getAppConfig().getOrDefault("PROJECT_PATHS", "~/Projects");
        String home = System.getProperty("user.home");

        for (String basePath : pathsStr.split(":")) {
            basePath = basePath.strip().replace("~", home);
            Path base = Path.of(basePath);
            if (!Files.isDirectory(base)) continue;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
                for (Path dir : stream) {
                    if (!Files.isDirectory(dir)) continue;
                    DiscoveredProject project = probe(dir);
                    if (project != null) {
                        projects.add(project);
                    }
                }
            } catch (IOException ignored) {
            }
        }

        projects.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return projects;
    }

    private DiscoveredProject probe(Path dir) {
        boolean hasClaudeDir = Files.isDirectory(dir.resolve(".claude"));
        boolean hasClaudeMd = Files.exists(dir.resolve("CLAUDE.md"));
        boolean hasMcpJson = Files.exists(dir.resolve(".mcp.json"));

        if (!hasClaudeDir && !hasClaudeMd && !hasMcpJson) return null;

        boolean hasProjectSettings = Files.exists(dir.resolve(".claude/settings.json"));
        boolean hasProjectLocalSettings = Files.exists(dir.resolve(".claude/settings.local.json"));
        boolean hasCommands = Files.isDirectory(dir.resolve(".claude/commands"));

        int permCount = 0;
        if (hasProjectSettings) {
            permCount += configService.getPermissions(dir.resolve(".claude/settings.json")).size();
        }
        if (hasProjectLocalSettings) {
            permCount += configService.getPermissions(dir.resolve(".claude/settings.local.json")).size();
        }

        return new DiscoveredProject(
                dir.toString(), dir.getFileName().toString(),
                hasClaudeDir, hasClaudeMd, hasMcpJson,
                hasProjectSettings, hasProjectLocalSettings, hasCommands,
                permCount);
    }
}
