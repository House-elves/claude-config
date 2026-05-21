package io.quarkus.houseelves.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ClaudeConfigService {

    static final Path CLAUDE_DIR = Path.of(System.getProperty("user.home"), ".claude");
    static final Path APP_CONFIG = Path.of(System.getProperty("user.home"), ".config", "claude-config", "config");

    @Inject
    ObjectMapper mapper;

    // --- JSON file operations ---

    public JsonNode readJsonFile(Path path) {
        try {
            if (Files.exists(path)) {
                return mapper.readTree(Files.readString(path));
            }
        } catch (IOException ignored) {
        }
        return mapper.createObjectNode();
    }

    public void writeJsonFile(Path path, JsonNode content) throws IOException {
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), content);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // --- Text file operations ---

    public String readTextFile(Path path) {
        try {
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (IOException ignored) {
        }
        return "";
    }

    public void writeTextFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // --- Path helpers ---

    public Path userSettingsPath() {
        return CLAUDE_DIR.resolve("settings.json");
    }

    public Path userLocalSettingsPath() {
        return CLAUDE_DIR.resolve("settings.local.json");
    }

    public Path projectSettingsPath(String projectPath) {
        return Path.of(projectPath, ".claude", "settings.json");
    }

    public Path projectLocalSettingsPath(String projectPath) {
        return Path.of(projectPath, ".claude", "settings.local.json");
    }

    public Path userClaudeMdPath() {
        return CLAUDE_DIR.resolve("CLAUDE.md");
    }

    public Path projectClaudeMdPath(String projectPath) {
        return Path.of(projectPath, "CLAUDE.md");
    }

    public Path userMcpPath() {
        return CLAUDE_DIR.resolve(".mcp.json");
    }

    public Path projectMcpPath(String projectPath) {
        return Path.of(projectPath, ".mcp.json");
    }

    public Path keybindingsPath() {
        return CLAUDE_DIR.resolve("keybindings.json");
    }

    public Path userCommandsDir() {
        return CLAUDE_DIR.resolve("commands");
    }

    public Path projectCommandsDir(String projectPath) {
        return Path.of(projectPath, ".claude", "commands");
    }

    // --- Commands ---

    public Map<String, String> readCommands(Path commandsDir) {
        Map<String, String> commands = new LinkedHashMap<>();
        if (!Files.isDirectory(commandsDir)) return commands;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(commandsDir, "*.md")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                name = name.substring(0, name.length() - 3); // strip .md
                commands.put(name, Files.readString(file));
            }
        } catch (IOException ignored) {
        }
        return commands;
    }

    // --- Permissions ---

    public List<String> getPermissions(Path settingsPath) {
        JsonNode root = readJsonFile(settingsPath);
        JsonNode perms = root.path("permissions").path("allow");
        List<String> result = new ArrayList<>();
        if (perms.isArray()) {
            for (JsonNode p : perms) {
                result.add(p.asText());
            }
        }
        return result;
    }

    public void addPermission(Path settingsPath, String pattern) throws IOException {
        JsonNode root = readJsonFile(settingsPath);
        ObjectNode rootObj = root.isObject() ? (ObjectNode) root : mapper.createObjectNode();
        ObjectNode permsObj = rootObj.has("permissions") ? (ObjectNode) rootObj.get("permissions") : mapper.createObjectNode();
        ArrayNode allow = permsObj.has("allow") ? (ArrayNode) permsObj.get("allow") : mapper.createArrayNode();

        for (JsonNode existing : allow) {
            if (existing.asText().equals(pattern)) return;
        }
        allow.add(pattern);
        permsObj.set("allow", allow);
        rootObj.set("permissions", permsObj);
        writeJsonFile(settingsPath, rootObj);
    }

    public boolean removePermission(Path settingsPath, String pattern) throws IOException {
        JsonNode root = readJsonFile(settingsPath);
        if (!root.isObject()) return false;
        ObjectNode rootObj = (ObjectNode) root;
        JsonNode permsNode = rootObj.path("permissions");
        if (!permsNode.isObject()) return false;
        JsonNode allowNode = permsNode.path("allow");
        if (!allowNode.isArray()) return false;

        ArrayNode allow = (ArrayNode) allowNode;
        boolean removed = false;
        for (int i = allow.size() - 1; i >= 0; i--) {
            if (allow.get(i).asText().equals(pattern)) {
                allow.remove(i);
                removed = true;
            }
        }
        if (removed) {
            writeJsonFile(settingsPath, rootObj);
        }
        return removed;
    }

    // --- App config ---

    public Map<String, String> getAppConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("PROJECT_PATHS", "~/Projects");
        if (!Files.exists(APP_CONFIG)) return config;
        try {
            for (String line : Files.readAllLines(APP_CONFIG)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    config.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip());
                }
            }
        } catch (IOException ignored) {
        }
        return config;
    }

    public void updateAppConfig(Map<String, String> updates) throws IOException {
        Map<String, String> config = getAppConfig();
        config.putAll(updates);
        Files.createDirectories(APP_CONFIG.getParent());
        StringBuilder sb = new StringBuilder();
        for (var entry : config.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        Path tmp = APP_CONFIG.resolveSibling("config.tmp");
        Files.writeString(tmp, sb.toString());
        Files.move(tmp, APP_CONFIG, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    // --- All watched paths (for file watcher) ---

    public List<Path> getWatchedPaths(List<String> projectPaths) {
        List<Path> paths = new ArrayList<>();
        paths.add(userSettingsPath());
        paths.add(userLocalSettingsPath());
        paths.add(userClaudeMdPath());
        paths.add(userMcpPath());
        paths.add(keybindingsPath());
        for (String proj : projectPaths) {
            paths.add(projectSettingsPath(proj));
            paths.add(projectLocalSettingsPath(proj));
            paths.add(projectClaudeMdPath(proj));
            paths.add(projectMcpPath(proj));
        }
        return paths;
    }
}
