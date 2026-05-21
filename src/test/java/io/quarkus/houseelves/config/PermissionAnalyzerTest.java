package io.quarkus.houseelves.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.houseelves.config.PermissionAnalyzer.StaleReason;

class PermissionAnalyzerTest {

    private final PermissionAnalyzer analyzer = new PermissionAnalyzer();

    @Test
    void detectsPidKill() {
        List<StaleReason> reasons = analyzer.detectStale("Bash(kill 751399)");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("pid")));
    }

    @Test
    void detectsTmpPath() {
        List<StaleReason> reasons = analyzer.detectStale("Bash(mkdir -p /tmp/vaadin-test-app/src/main/java/org/acme)");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("tmp")));
    }

    @Test
    void detectsHardcodedToken() {
        List<StaleReason> reasons = analyzer.detectStale("Bash(GH_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx gh issue *)");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("token")));
    }

    @Test
    void detectsDateInPath() {
        List<StaleReason> reasons = analyzer.detectStale("Bash(mediainfo \"/home/pkruger/Videos/2026-03-22 14-22-04.mkv\")");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("date")));
    }

    @Test
    void detectsOneOffRpm() {
        List<StaleReason> reasons = analyzer.detectStale("Bash(rpm -q dragon)");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("rpm")));
    }

    @Test
    void detectsSystemPathRead() {
        List<StaleReason> reasons = analyzer.detectStale("Read(//sys/bus/usb/devices/**)");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("syspath")));
    }

    @Test
    void detectsSpecificPkill() {
        List<StaleReason> reasons = analyzer.detectStale("Bash(pkill -f \"vaadin-test-app\")");
        assertTrue(reasons.stream().anyMatch(r -> r.code().equals("pkill")));
    }

    @Test
    void wildcardedPermissionsAreNotStale() {
        assertTrue(analyzer.detectStale("Bash(git *)").isEmpty());
        assertTrue(analyzer.detectStale("Bash(gh *)").isEmpty());
        assertTrue(analyzer.detectStale("mcp__quarkus-agent__*").isEmpty());
        assertTrue(analyzer.detectStale("Bash(mvn jackknife:*)").isEmpty());
    }

    @Test
    void generalPermissionsAreNotStale() {
        assertTrue(analyzer.detectStale("Bash(curl *)").isEmpty());
        assertTrue(analyzer.detectStale("WebSearch").isEmpty());
        assertTrue(analyzer.detectStale("mcp__playwright__*").isEmpty());
    }

    @Test
    void analyzeReturnsAnnotatedResults() {
        List<String> patterns = List.of("Bash(git *)", "Bash(kill 12345)", "Bash(gh *)");
        var results = analyzer.analyze(patterns, "user", null);
        assertEquals(3, results.size());
        assertFalse(results.get(0).stale());
        assertTrue(results.get(1).stale());
        assertFalse(results.get(2).stale());
        assertEquals("user", results.get(0).level());
    }
}
