package io.quarkus.houseelves.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.inject.Singleton;

@Singleton
public class PermissionAnalyzer {

    public record StaleReason(String code, String description) {}

    public record AnalyzedPermission(
            String pattern,
            String level,
            String projectPath,
            boolean stale,
            List<StaleReason> reasons) {}

    private static final Pattern PID_KILL = Pattern.compile("Bash\\(kill \\d{4,}\\)");
    private static final Pattern TMP_PATH = Pattern.compile("/tmp/");
    private static final Pattern HARDCODED_TOKEN = Pattern.compile("(ghp_|gho_|sk-|token=)\\w{10,}");
    private static final Pattern DATE_IN_PATH = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern ONE_OFF_RPM = Pattern.compile("Bash\\(rpm -q[iRl]? [a-zA-Z][a-zA-Z0-9_-]+\\)$");
    private static final Pattern SYSTEM_PATH_READ = Pattern.compile("Read\\(//(?:sys|usr)/");
    private static final Pattern SPECIFIC_PKILL = Pattern.compile("Bash\\(pkill.*\"[^*]+\"\\)");
    private static final Pattern DEEP_ABSOLUTE_NO_WILDCARD = Pattern.compile(
            "Bash\\([^)]*(/[^/]+){5,}[^*)]\\)$");

    public List<AnalyzedPermission> analyze(List<String> patterns, String level, String projectPath) {
        List<AnalyzedPermission> results = new ArrayList<>();
        for (String pattern : patterns) {
            List<StaleReason> reasons = detectStale(pattern);
            results.add(new AnalyzedPermission(pattern, level, projectPath, !reasons.isEmpty(), reasons));
        }
        return results;
    }

    List<StaleReason> detectStale(String pattern) {
        List<StaleReason> reasons = new ArrayList<>();

        if (PID_KILL.matcher(pattern).find()) {
            reasons.add(new StaleReason("pid", "Contains specific process ID"));
        }
        if (TMP_PATH.matcher(pattern).find()) {
            reasons.add(new StaleReason("tmp", "References /tmp/ path"));
        }
        if (HARDCODED_TOKEN.matcher(pattern).find()) {
            reasons.add(new StaleReason("token", "Contains hardcoded token/credential"));
        }
        if (DATE_IN_PATH.matcher(pattern).find()) {
            reasons.add(new StaleReason("date", "Contains date-specific file path"));
        }
        if (ONE_OFF_RPM.matcher(pattern).matches()) {
            reasons.add(new StaleReason("rpm", "One-off package query"));
        }
        if (SYSTEM_PATH_READ.matcher(pattern).find()) {
            reasons.add(new StaleReason("syspath", "System path read (debug session)"));
        }
        if (SPECIFIC_PKILL.matcher(pattern).find()) {
            reasons.add(new StaleReason("pkill", "Kill specific process by name"));
        }
        if (DEEP_ABSOLUTE_NO_WILDCARD.matcher(pattern).find() && !pattern.contains("*")) {
            reasons.add(new StaleReason("specific", "Very specific absolute path"));
        }

        return reasons;
    }
}
