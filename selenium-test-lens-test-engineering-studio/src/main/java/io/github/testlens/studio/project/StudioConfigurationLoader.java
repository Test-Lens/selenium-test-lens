package io.github.testlens.studio.project;

import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/** Loads the commit-safe project configuration and the machine-local Studio overrides. @since 0.5.0 */
public final class StudioConfigurationLoader {
    public static final int SCHEMA_VERSION = 1;
    private static final Set<String> LOCAL_FIELDS = Set.of("schemaVersion", "browser", "agents", "codexExecutable");
    private static final Set<String> BROWSER_FIELDS = Set.of("name", "headless", "provider", "profile");
    private static final Set<String> AGENT_FIELDS = Set.of("provider", "profiles");
    private static final Set<String> SECRET_NAMES = Set.of("password", "passwd", "secret", "token", "cookie",
            "authorization", "credential", "apikey", "api_key", "bearer", "jwt", "clientsecret");

    public EffectiveConfiguration load(Path projectRoot) {
        return load(projectRoot, CliOverrides.none());
    }

    public EffectiveConfiguration load(Path projectRoot, CliOverrides cli) {
        Path root = canonicalRoot(projectRoot);
        rejectWorkspaceSymlink(root);
        Map<String, Object> project = read(root.resolve(".test-lens/project.json"), false);
        Map<String, Object> local = read(root.resolve(".test-lens/local.json"), true);
        validateSchema(project, "project.json");
        validateSchema(local, "local.json");
        rejectSecrets(project, "project config");
        rejectSecrets(local, "local config");
        ProjectDiscovery.rejectUnknownFields(local, LOCAL_FIELDS, "local config");
        Map<String, Object> localBrowser = object(local, "browser");
        Map<String, Object> localAgents = object(local, "agents");
        ProjectDiscovery.rejectUnknownFields(localBrowser, BROWSER_FIELDS, "local browser config");
        ProjectDiscovery.rejectUnknownFields(localAgents, AGENT_FIELDS, "local agent config");

        Map<String, Object> projectBrowser = object(project, "browser");
        Map<String, String> projectProfiles = stringsMap(project.get("agentProfiles"), "agentProfiles");
        Map<String, String> localProfiles = stringsMap(localAgents.get("profiles"), "agents.profiles");
        Map<String, String> profiles = new TreeMap<>(projectProfiles);
        profiles.putAll(localProfiles);
        validateProfiles(profiles);

        String browser = first(cli.browser(), text(localBrowser.get("name")), browserName(projectBrowser, project), "CHROME");
        Boolean localHeadless = bool(localBrowser.get("headless"), "browser.headless");
        Boolean projectHeadless = bool(projectBrowser.get("headless"), "browser.headless");
        if (projectHeadless == null) projectHeadless = bool(project.get("headless"), "headless");
        boolean headless = firstBoolean(cli.headless(), localHeadless, projectHeadless, true);
        String browserProvider = first(cli.browserProvider(), text(localBrowser.get("provider")), "LOCAL");
        String browserProfile = first(cli.browserProfile(), text(localBrowser.get("profile")),
                text(project.get("browserProfile")), "default");
        String agentProvider = first(cli.agentProvider(), text(localAgents.get("provider")), "CODEX");
        Path executable = resolveExecutable(first(cli.codexExecutable(), text(local.get("codexExecutable"))));

        List<String> evidence = List.of(
                project.isEmpty() ? "project configuration absent" : "project configuration loaded",
                local.isEmpty() ? "local machine configuration absent" : "local machine configuration loaded",
                cli.hasValues() ? "CLI overrides applied" : "CLI overrides absent");
        return new EffectiveConfiguration(SCHEMA_VERSION, browser.toUpperCase(Locale.ROOT), headless,
                browserProvider, browserProfile, agentProvider, profiles, executable, evidence);
    }

    private static void rejectWorkspaceSymlink(Path root) {
        Path workspace=root.resolve(".test-lens");
        if(Files.exists(workspace,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(workspace))
            throw new IllegalArgumentException(".test-lens workspace must not be a symbolic link");
    }

    private static Map<String, Object> read(Path path, boolean local) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return Map.of();
        if (Files.isSymbolicLink(path)) throw new IllegalArgumentException((local ? "Local" : "Project") + " config must not be a symbolic link");
        try { return StrictJson.readObject(Files.readAllBytes(path)); }
        catch (IOException failure) { throw new IllegalArgumentException("Configuration cannot be read: " + path.getFileName(), failure); }
    }

    private static void validateSchema(Map<String, Object> value, String label) {
        if (value.isEmpty()) return;
        Object schema = value.get("schemaVersion");
        if (!(schema instanceof Number number) || number.intValue() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported " + label + " schema");
        }
    }

    private static Path canonicalRoot(Path value) {
        try {
            Path absolute = Objects.requireNonNull(value, "projectRoot").toAbsolutePath().normalize();
            if (Files.isSymbolicLink(absolute)) throw new IllegalArgumentException("Project root must not be a symbolic link");
            return absolute.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException failure) { throw new IllegalArgumentException("Project root cannot be resolved", failure); }
    }

    private static Path resolveExecutable(String value) {
        if (value == null) return null;
        Path path = Path.of(value);
        if (!path.isAbsolute()) throw new IllegalArgumentException("codexExecutable must be an absolute path");
        return path.normalize();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException(key + " must be an object");
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((name, item) -> result.put(Objects.toString(name), item));
        return Map.copyOf(result);
    }

    private static Map<String, String> stringsMap(Object value, String label) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException(label + " must be an object");
        Map<String, String> result = new TreeMap<>();
        map.forEach((key, item) -> {
            if (!(item instanceof String text) || text.isBlank()) throw new IllegalArgumentException(label + " values must be non-blank strings");
            result.put(Objects.toString(key).toUpperCase(Locale.ROOT), text.trim());
        });
        return Map.copyOf(result);
    }

    private static void validateProfiles(Map<String,String> profiles){
        Set<String> roles=java.util.Arrays.stream(AgentExecutor.Role.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());
        profiles.forEach((role,profile)->{if(!roles.contains(role))throw new IllegalArgumentException("Unknown agent role: "+role);if(!profile.matches("[A-Za-z0-9._-]{1,96}"))throw new IllegalArgumentException("Invalid logical agent profile for "+role);});
    }

    private static void rejectSecrets(Object value, String path) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String name = Objects.toString(entry.getKey());
                String normalized = name.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
                if (SECRET_NAMES.stream().anyMatch(normalized::contains)) throw new IllegalArgumentException("CONFIG_REJECTED_SECRET_LIKE_VALUE at " + path + "." + name);
                rejectSecrets(entry.getValue(), path + "." + name);
            }
        } else if (value instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) rejectSecrets(list.get(index), path + "[" + index + "]");
        } else if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.matches("(?i)^(bearer\\s+.+|eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\..+)$")) {
                throw new IllegalArgumentException("CONFIG_REJECTED_SECRET_LIKE_VALUE at " + path);
            }
        }
    }

    private static String browserName(Map<String, Object> browser, Map<String, Object> project) {
        String nested = text(browser.get("name"));
        Object value = project.get("browser");
        return nested != null ? nested : value instanceof String text ? text : null;
    }

    private static String text(Object value) { return value instanceof String text && !text.isBlank() ? text.trim() : null; }
    private static Boolean bool(Object value, String label) {
        if (value == null) return null;
        if (!(value instanceof Boolean result)) throw new IllegalArgumentException(label + " must be boolean");
        return result;
    }
    private static String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value.trim(); return null; }
    private static boolean firstBoolean(Boolean first, Boolean second, Boolean third, boolean fallback) {
        return first != null ? first : second != null ? second : third != null ? third : fallback;
    }

    public record CliOverrides(String browser, Boolean headless, String browserProvider, String browserProfile,
                               String agentProvider, String codexExecutable) {
        public static CliOverrides none() { return new CliOverrides(null, null, null, null, null, null); }
        boolean hasValues() { return browser != null || headless != null || browserProvider != null
                || browserProfile != null || agentProvider != null || codexExecutable != null; }
    }

    public record EffectiveConfiguration(int schemaVersion, String browser, boolean headless,
                                         String browserProvider, String browserProfile, String agentProvider,
                                         Map<String, String> agentProfiles, Path codexExecutable,
                                         List<String> evidence) {
        public EffectiveConfiguration {
            agentProfiles = Map.copyOf(agentProfiles == null ? Map.of() : agentProfiles);
            evidence = List.copyOf(evidence == null ? List.of() : evidence);
        }
    }
}
