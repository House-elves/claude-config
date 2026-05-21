package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.websockets.next.OpenConnections;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ConfigFileWatcher {

    private final ConcurrentHashMap<Path, FileTime> lastModified = new ConcurrentHashMap<>();

    @Inject
    OpenConnections connections;

    @Inject
    ClaudeConfigService configService;

    @Inject
    ProjectDiscoveryService discoveryService;

    @Inject
    ObjectMapper mapper;

    @Scheduled(every = "3s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void pollConfigFiles() {
        long clientCount = connections.stream().count();
        if (clientCount == 0) return;

        List<String> projectPaths = discoveryService.discoverAll().stream()
                .map(ProjectDiscoveryService.DiscoveredProject::path)
                .toList();
        List<Path> watchedPaths = configService.getWatchedPaths(projectPaths);

        boolean anyChanged = false;
        String changedFile = null;

        for (Path path : watchedPaths) {
            try {
                if (!Files.exists(path)) {
                    if (lastModified.remove(path) != null) {
                        anyChanged = true;
                        changedFile = path.toString();
                    }
                    continue;
                }
                FileTime current = Files.getLastModifiedTime(path);
                FileTime previous = lastModified.put(path, current);
                if (previous != null && current.compareTo(previous) > 0) {
                    anyChanged = true;
                    changedFile = path.toString();
                }
            } catch (IOException ignored) {
            }
        }

        if (anyChanged) {
            try {
                String payload = mapper.writeValueAsString(Map.of(
                        "type", "config-change",
                        "changedPath", changedFile != null ? changedFile : "",
                        "projects", discoveryService.discoverAll()));
                Log.infof("Config changed (%s), pushing to %d client(s)", changedFile, clientCount);
                connections.forEach(c -> c.sendTextAndAwait(payload));
            } catch (Exception e) {
                Log.warnf("Failed to push config change: %s", e.getMessage());
            }
        }
    }
}
