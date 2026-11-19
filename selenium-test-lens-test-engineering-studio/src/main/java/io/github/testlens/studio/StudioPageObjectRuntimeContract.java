package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.json.StrictJson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the bounded, deterministic Page Object acquisition contract for Studio's forked JUnit runtime. */
final class StudioPageObjectRuntimeContract {
    enum Feasibility { VERIFIED, INVALID, UNKNOWN }
    private static final Pattern QUALIFIED_CALL = Pattern.compile(
            "\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\.\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(");
    private static final Set<String> ASSERTION_METHODS = Set.of(
            "assertTrue", "assertFalse", "assertEquals", "assertNotEquals", "assertNull", "assertNotNull",
            "assertSame", "assertNotSame", "assertThrows", "assertDoesNotThrow", "assertAll", "fail");
    private static final String DRIVER_CONTEXT =
            "io.github.testlens.studio.browser.BrowserSessionContext.currentDriver()";

    private StudioPageObjectRuntimeContract() { }

    static String render(AgentContextPack context) {
        AgentContextPack.PageObjectCapabilityManifest manifest = context.pageObjectCapabilities();
        List<Map<String, Object>> classes = new ArrayList<>();
        for (AgentContextPack.PageObjectCapability capability : manifest.classes()) {
            List<Map<String, Object>> constructors = capability.constructors().stream()
                    .map(value -> constructor(capability.qualifiedName(), value))
                    .toList();
            List<Map<String, Object>> methods = capability.methods().stream().map(value -> {
                Map<String, Object> method = new LinkedHashMap<>();
                method.put("name", value.name());
                method.put("signature", value.signature());
                method.put("apiReference", CanonicalPageObjectApiReference.of(capability.qualifiedName(), value).render());
                if (value.declaringType() != null) method.put("declaringType", value.declaringType());
                method.put("returnType", value.returnType());
                method.put("static", value.staticMethod());
                method.put("assertionCompatibility", assertionCompatibility(value.returnType()));
                return Map.copyOf(method);
            }).toList();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("qualifiedName", capability.qualifiedName());
            entry.put("constructors", constructors);
            entry.put("methods", methods);
            classes.add(Map.copyOf(entry));
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("sourceFingerprint", manifest.sourceFingerprint());
        root.put("manifestCompleteness", manifest.completeness().name());
        root.put("runtime", "STUDIO_FORKED_JUNIT_5");
        root.put("runtimeGuarantees", List.of("STUDIO_BOUND_BROWSER_SESSION"));
        root.put("allowedPageObjectApiReferences", CanonicalPageObjectApiReference.allowed(manifest).stream()
                .map(CanonicalPageObjectApiReference::render).toList());
        root.put("classes", List.copyOf(classes));
        return new String(StrictJson.write(root), StandardCharsets.UTF_8);
    }

    static Feasibility assess(TestPlan plan, AgentContextPack context) {
        AgentContextPack.PageObjectCapabilityManifest manifest = context.pageObjectCapabilities();
        if (manifest.completeness() != AgentContextPack.ManifestCompleteness.COMPLETE
                || manifest.sourceFingerprint() == null || manifest.sourceFingerprint().isBlank()) {
            return Feasibility.UNKNOWN;
        }
        List<String> fragments = new ArrayList<>();
        for (TestPlan.TestScenario scenario : plan.scenarios()) {
            scenario.steps().forEach(value -> fragments.add(String.valueOf(value.action())));
            scenario.expected().forEach(value -> fragments.add(String.valueOf(value.assertion())));
        }
        Map<String, List<PageOwner>> owners = new LinkedHashMap<>();
        for (AgentContextPack.PageObjectCapability owner : manifest.classes()) {
            boolean constructible = owner.constructors().stream()
                    .map(value -> constructor(owner.qualifiedName(), value))
                    .anyMatch(value -> value.containsKey("constructionExpression"));
            PageOwner pageOwner = new PageOwner(owner.methods(), constructible);
            String simpleName = owner.qualifiedName().substring(owner.qualifiedName().lastIndexOf('.') + 1);
            owners.computeIfAbsent(simpleName, ignored -> new ArrayList<>()).add(pageOwner);
            String variableName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
            owners.computeIfAbsent(variableName, ignored -> new ArrayList<>()).add(pageOwner);
        }
        boolean matched = false;
        for (String fragment : fragments) {
            Matcher calls = QUALIFIED_CALL.matcher(fragment);
            while (calls.find()) {
                String receiver = calls.group(1);
                String name = calls.group(2);
                if (ASSERTION_METHODS.contains(name)) continue;
                List<PageOwner> pageOwners = owners.get(receiver);
                if (pageOwners == null || pageOwners.size() != 1) return Feasibility.UNKNOWN;
                int argumentCount = argumentCount(fragment, calls.end());
                if (argumentCount < 0) return Feasibility.UNKNOWN;
                PageOwner pageOwner = pageOwners.get(0);
                List<AgentContextPack.MethodCapability> candidates = pageOwner.methods().stream()
                        .filter(value -> value.name().equals(name) && value.parameters().size() == argumentCount)
                        .toList();
                if (candidates.isEmpty()) return Feasibility.INVALID;
                if (candidates.size() != 1) return Feasibility.UNKNOWN;
                matched = true;
                AgentContextPack.MethodCapability candidate = candidates.get(0);
                if (!candidate.staticMethod() && !pageOwner.constructible()) return Feasibility.INVALID;
                if (usedAsAssertTrueValue(fragment, name) && "void".equals(candidate.returnType())) {
                    return Feasibility.INVALID;
                }
            }
        }
        return matched ? Feasibility.VERIFIED : Feasibility.UNKNOWN;
    }

    private static boolean usedAsAssertTrueValue(String text, String methodName) {
        return Pattern.compile("\\bassertTrue\\s*\\([^;]*\\.\\s*" + Pattern.quote(methodName) + "\\s*\\(")
                .matcher(text).find();
    }

    private static int argumentCount(String value, int afterOpeningParenthesis) {
        int depth = 1;
        int commas = 0;
        boolean content = false;
        boolean quoted = false;
        char quote = 0;
        for (int index = afterOpeningParenthesis; index < value.length(); index++) {
            char current = value.charAt(index);
            if (quoted) {
                if (current == '\\') index++;
                else if (current == quote) quoted = false;
                content = true;
                continue;
            }
            if (current == '\'' || current == '"') {
                quoted = true;
                quote = current;
                content = true;
            } else if (current == '(') {
                depth++;
                content = true;
            } else if (current == ')' && --depth == 0) {
                return content ? commas + 1 : 0;
            } else if (current == ',' && depth == 1) {
                commas++;
            } else if (!Character.isWhitespace(current)) {
                content = true;
            }
        }
        return -1;
    }

    private record PageOwner(List<AgentContextPack.MethodCapability> methods, boolean constructible) { }

    private static Map<String, Object> constructor(String qualifiedName,
                                                   AgentContextPack.ConstructorCapability value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("signature", value.signature());
        String expression = null;
        String acquisition = "UNSUPPORTED_OR_INACCESSIBLE";
        if ("PUBLIC".equals(value.accessibility()) && "DIRECT_CONSTRUCTION".equals(value.acquisition())) {
            if (value.parameters().isEmpty()) {
                acquisition = "DIRECT_NO_ARGS";
                expression = "new " + qualifiedName + "()";
            } else if (value.parameters().size() == 1 && webDriver(value.parameters().get(0).type())) {
                acquisition = "STUDIO_BOUND_WEBDRIVER";
                expression = "new " + qualifiedName + "(" + DRIVER_CONTEXT + ")";
            } else {
                acquisition = "UNRESOLVED_DEPENDENCIES";
            }
        }
        result.put("acquisition", acquisition);
        if (expression != null) result.put("constructionExpression", expression);
        return Map.copyOf(result);
    }

    private static boolean webDriver(String type) {
        return "org.openqa.selenium.WebDriver".equals(type);
    }

    private static String assertionCompatibility(String type) {
        if ("boolean".equals(type) || "Boolean".equals(type) || "java.lang.Boolean".equals(type)) {
            return "BOOLEAN_ASSERTION_COMPATIBLE";
        }
        return "void".equals(type) ? "VOID_NOT_VALUE_ASSERTABLE" : "VALUE_ASSERTION_REQUIRES_TYPE_COMPATIBILITY";
    }
}
