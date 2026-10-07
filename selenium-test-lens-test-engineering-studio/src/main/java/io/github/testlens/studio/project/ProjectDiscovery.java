package io.github.testlens.studio.project;

import io.github.testlens.application.tooling.json.StrictJson;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Secure deterministic discovery of Studio project configuration. @since 0.5.0 */
public final class ProjectDiscovery {
    private static final Set<String> PROJECT_FIELDS = Set.of(
            "schemaVersion", "projectId", "applicationName", "name", "sourceRoots", "classpathEntries",
            "workspaceDirectory", "workspace", "startUrl", "browser", "headless", "pageObjectPackages",
            "browserProfile", "agentProfiles");
    private static final Set<String> SECRET_NAMES = Set.of(
            "password", "passwd", "secret", "token", "cookie", "authorization",
            "credential", "apikey", "api_key");

    public ProjectDescriptor discover(Path projectRoot) {
        return discover(new Request(projectRoot, Overrides.none()));
    }

    public ProjectDescriptor discover(Request request) {
        Objects.requireNonNull(request, "request");
        Path root = canonicalRoot(request.projectRoot());
        Map<String, Object> config = readConfiguration(root);
        rejectUnknownFields(config, PROJECT_FIELDS, "project config");
        rejectSecretLike(config, "config");
        int schema = integer(config, "schemaVersion", ProjectDescriptor.SCHEMA_VERSION);
        if (schema != ProjectDescriptor.SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported project schema " + schema);
        }

        Autodetected detected = autodetect(root);
        Overrides cli = request.overrides() == null ? Overrides.none() : request.overrides();
        String configuredName = string(config, "applicationName", string(config, "name", null));
        String name = first(cli.applicationName(), configuredName, detected.name(), root.getFileName().toString());
        List<Path> sources = cli.sourceRoots() != null ? resolveRoots(root, cli.sourceRoots(), "source root")
                : config.containsKey("sourceRoots") ? resolveRoots(root, strings(config, "sourceRoots"), "source root") : detected.sourceRoots();
        List<Path> mainRoots=sources.stream().filter(path->!path.toString().replace('\\','/').contains("/test/")).toList();
        List<Path> testRoots=sources.stream().filter(path->path.toString().replace('\\','/').contains("/test/")).toList();
        List<Path> classpath = cli.classpathEntries() != null ? resolveMavenClasspath(root, cli.classpathEntries())
                : config.containsKey("classpathEntries") ? resolveRoots(root, strings(config, "classpathEntries"), "classpath entry")
                : detected.classpathEntries();
        String workspaceValue = first(pathText(cli.workspaceDirectory()), string(config, "workspaceDirectory",
                string(config, "workspace", null)), ".test-lens");
        Path workspace = resolveInside(root, workspaceValue, "workspace directory");
        String start = first(cli.startUrl(), string(config, "startUrl", null));
        URI startUrl = start == null ? null : safeUrl(start);
        ProjectDescriptor.Browser browser = parseBrowser(first(cli.browser(), browserName(config), "CHROME"));
        boolean headless = cli.headless() != null ? cli.headless() : browserHeadless(config, true);

        ProjectDescriptor.ConfigurationSource source = cli.hasValues()
                ? ProjectDescriptor.ConfigurationSource.CLI_OVERRIDE
                : config.isEmpty() ? ProjectDescriptor.ConfigurationSource.AUTO_DETECTED
                : ProjectDescriptor.ConfigurationSource.PROJECT_CONFIG;
        List<String> evidence = new ArrayList<>(detected.evidence());
        evidence.add("project root resolved without symbolic links");
        if (!config.isEmpty()) evidence.add("loaded .test-lens/project.json schema 1");
        List<String> limitations = new ArrayList<>();
        if (detected.buildSystem() == ProjectDescriptor.BuildSystem.UNKNOWN) limitations.add("BUILD_SYSTEM_NOT_DETECTED");
        if (sources.isEmpty()) limitations.add("NO_SOURCE_ROOTS_DISCOVERED");
        if (startUrl == null) limitations.add("START_URL_NOT_CONFIGURED");
        ProjectDescriptor.Status status = detected.buildSystem()==ProjectDescriptor.BuildSystem.GRADLE
                ? ProjectDescriptor.Status.UNSUPPORTED_PROJECT:sources.isEmpty()?ProjectDescriptor.Status.NEEDS_CONFIGURATION:ProjectDescriptor.Status.READY;
        String explicitId = first(cli.projectId(), string(config, "projectId", null));
        String projectId = explicitId == null ? stableId(name, detected.buildSystem()) : validateProjectId(explicitId);
        return new ProjectDescriptor(schema, projectId, name, root, detected.buildSystem(), status, source,
                mainRoots,testRoots, classpath, workspace, startUrl,
                new ProjectDescriptor.BrowserFlags(browser, headless), evidence, limitations);
    }

    public record Request(Path projectRoot, Overrides overrides) {
        public Request { Objects.requireNonNull(projectRoot, "projectRoot"); }
    }

    /** Nullable values mean that the lower-precedence config/autodetection value remains in effect. */
    public record Overrides(String projectId, String applicationName, List<Path> sourceRoots,
                            List<Path> classpathEntries, Path workspaceDirectory, String startUrl,
                            String browser, Boolean headless) {
        public Overrides {
            sourceRoots = sourceRoots == null ? null : List.copyOf(sourceRoots);
            classpathEntries = classpathEntries == null ? null : List.copyOf(classpathEntries);
        }
        public static Overrides none() { return new Overrides(null, null, null, null, null, null, null, null); }
        boolean hasValues() { return projectId != null || applicationName != null || sourceRoots != null
                || classpathEntries != null || workspaceDirectory != null || startUrl != null || browser != null || headless != null; }
    }

    private static Path canonicalRoot(Path requested) {
        try {
            Path absolute = requested.toAbsolutePath().normalize();
            if (Files.isSymbolicLink(absolute)) throw new IllegalArgumentException("Project root must not be a symbolic link");
            Path real = absolute.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!Files.isDirectory(real, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Project root is not a directory");
            return real;
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("Project root cannot be resolved", failure);
        }
    }

    private static Map<String, Object> readConfiguration(Path root) {
        Path directory = root.resolve(".test-lens");
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(directory)) {
            throw new IllegalArgumentException("Project config directory must not be a symbolic link");
        }
        Path path = directory.resolve("project.json");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return Map.of();
        if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("Project config must not be a symbolic link");
        try {
            return StrictJson.readObject(Files.readAllBytes(path));
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("Project config cannot be read", failure);
        }
    }

    private static Autodetected autodetect(Path root) {
        if (Files.isRegularFile(root.resolve("pom.xml"), LinkOption.NOFOLLOW_LINKS)) {
            String name = mavenName(root.resolve("pom.xml"));
            return new Autodetected(ProjectDescriptor.BuildSystem.MAVEN, name,
                    existing(root, "src/main/java", "src/test/java"),
                    existing(root, "target/classes", "target/test-classes"), List.of("detected Maven pom.xml"));
        }
        if (Files.isRegularFile(root.resolve("build.gradle"), LinkOption.NOFOLLOW_LINKS)
                || Files.isRegularFile(root.resolve("build.gradle.kts"), LinkOption.NOFOLLOW_LINKS)) {
            return new Autodetected(ProjectDescriptor.BuildSystem.GRADLE, root.getFileName().toString(),
                    existing(root, "src/main/java", "src/test/java"),
                    existing(root, "build/classes/java/main", "build/classes/java/test"), List.of("detected Gradle build"));
        }
        return new Autodetected(ProjectDescriptor.BuildSystem.UNKNOWN, root.getFileName().toString(),
                existing(root, "src/main/java", "src/test/java"), List.of(), List.of());
    }

    private static String mavenName(Path pom) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
            var project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
            String name = directChildText(project, "name");
            if (name != null) return name;
            String artifact = directChildText(project, "artifactId");
            return artifact == null ? pom.getParent().getFileName().toString() : artifact;
        } catch (Exception ignored) {
            return pom.getParent().getFileName().toString();
        }
    }

    private static String directChildText(org.w3c.dom.Element parent, String name) {
        for (var child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE
                    && name.equals(child.getLocalName() == null ? child.getNodeName() : child.getLocalName())) {
                String value = child.getTextContent();
                if (value != null && !value.isBlank()) return value.trim();
            }
        }
        return null;
    }

    private static List<Path> existing(Path root, String... values) {
        List<Path> paths = new ArrayList<>();
        for (String value : values) {
            Path path = resolveInside(root, value, "autodetected root");
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) paths.add(path);
        }
        return List.copyOf(paths);
    }

    private static List<Path> resolveRoots(Path root, List<?> values, String kind) {
        List<Path> paths = new ArrayList<>();
        for (Object value : values) paths.add(resolveInside(root, Objects.toString(value), kind));
        return List.copyOf(paths);
    }

    private static List<Path> resolveMavenClasspath(Path root,List<Path> values){
        String configured=System.getProperty("maven.repo.local");
        Path repository=(configured==null||configured.isBlank()?Path.of(System.getProperty("user.home"),".m2","repository"):Path.of(configured)).toAbsolutePath().normalize();
        List<Path> result=new ArrayList<>();
        for(Path value:values){
            Path path=(value.isAbsolute()?value:root.resolve(value)).toAbsolutePath().normalize();
            if(!path.startsWith(root)&&!path.startsWith(repository))throw new IllegalArgumentException("classpath entry is outside the project and Maven repository");
            if(Files.isSymbolicLink(path))throw new IllegalArgumentException("classpath entry must not be a symbolic link");
            result.add(path);
        }
        return List.copyOf(result);
    }

    private static Path resolveInside(Path root, String value, String kind) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(kind + " is blank");
        Path input = Path.of(value);
        Path path = (input.isAbsolute() ? input : root.resolve(input)).toAbsolutePath().normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException(kind + " escapes project root");
        try {
            Path cursor = path;
            while (cursor != null && cursor.startsWith(root)) {
                if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(cursor)) {
                    throw new IllegalArgumentException(kind + " contains a symbolic link");
                }
                if (cursor.equals(root)) break;
                cursor = cursor.getParent();
            }
        } catch (SecurityException failure) {
            throw new IllegalArgumentException(kind + " cannot be validated", failure);
        }
        return path;
    }

    private static URI safeUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!uri.isAbsolute() || uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) throw new IllegalArgumentException("startUrl must be absolute HTTP(S)");
            if (uri.getUserInfo() != null || hasSecretQuery(uri.getRawQuery())) throw new IllegalArgumentException("startUrl contains credentials or secret-like query data");
            return uri;
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Invalid startUrl", failure);
        }
    }

    private static boolean hasSecretQuery(String query) {
        if (query == null) return false;
        for (String pair : query.split("&")) {
            String key = pair.split("=", 2)[0].toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
            if (SECRET_NAMES.stream().map(value -> value.replace("_", "")).anyMatch(key::contains)) return true;
        }
        return false;
    }

    private static void rejectSecretLike(Object value, String path) {
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                String key = Objects.toString(entry.getKey());
                String normalized = key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
                if (SECRET_NAMES.stream().map(item -> item.replace("_", "")).anyMatch(normalized::contains)) {
                    throw new IllegalArgumentException("Secret-like project config field is forbidden: " + path + "." + key);
                }
                rejectSecretLike(entry.getValue(), path + "." + key);
            }
        } else if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) rejectSecretLike(list.get(i), path + "[" + i + "]");
        } else if(value instanceof String text){
            String trimmed=text.trim();if(trimmed.matches("(?i)^(bearer\\s+.+|eyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\..+)$")||trimmed.matches("(?i).*(password|secret|authorization|client_secret)\\s*[:=].+"))throw new IllegalArgumentException("CONFIG_REJECTED_SECRET_LIKE_VALUE at "+path);
        }
    }

    static void rejectUnknownFields(Map<String, Object> value, Set<String> allowed, String label) {
        List<String> unknown = value.keySet().stream().filter(key -> !allowed.contains(key)).sorted().toList();
        if (!unknown.isEmpty()) throw new IllegalArgumentException(label + " contains unknown fields: " + unknown);
    }

    private static ProjectDescriptor.Browser parseBrowser(String value) {
        try { return ProjectDescriptor.Browser.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (RuntimeException failure) { throw new IllegalArgumentException("Unsupported browser: " + value, failure); }
    }

    private static String browserName(Map<String, Object> config) {
        Object value = config.get("browser");
        if (value instanceof String text) return text;
        if (value instanceof Map<?, ?> map) return Objects.toString(map.get("name"), null);
        return null;
    }

    private static boolean browserHeadless(Map<String, Object> config, boolean fallback) {
        Object value = config.get("browser");
        if (!(value instanceof Map<?, ?> map) || !map.containsKey("headless")) {
            Object topLevel = config.get("headless");
            if (topLevel == null) return fallback;
            if (!(topLevel instanceof Boolean result)) throw new IllegalArgumentException("headless must be boolean");
            return result;
        }
        Object headless = map.get("headless");
        if (!(headless instanceof Boolean result)) throw new IllegalArgumentException("browser.headless must be boolean");
        return result;
    }

    private static String stableId(String name, ProjectDescriptor.BuildSystem build) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (name.trim().toLowerCase(Locale.ROOT) + "\n" + build.name()).getBytes(StandardCharsets.UTF_8));
            return "project-v1:" + HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String validateProjectId(String value) {
        if (!value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{2,127}")) throw new IllegalArgumentException("Invalid projectId");
        return value;
    }

    private static String string(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        if (value == null) return fallback;
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException(key + " must be a non-blank string");
        return text;
    }

    private static int integer(Map<String, Object> values, String key, int fallback) {
        Object value = values.get(key);
        if (value == null) return fallback;
        if (!(value instanceof Number number)) throw new IllegalArgumentException(key + " must be an integer");
        return number.intValue();
    }

    private static List<String> strings(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException(key + " must be an array");
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text) || text.isBlank()) throw new IllegalArgumentException(key + " contains an invalid path");
            result.add(text);
        }
        return result;
    }

    private static String pathText(Path path) { return path == null ? null : path.toString(); }
    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private record Autodetected(ProjectDescriptor.BuildSystem buildSystem, String name, List<Path> sourceRoots,
                                List<Path> classpathEntries, List<String> evidence) { }
}
