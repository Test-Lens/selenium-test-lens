package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing;
import io.github.testlens.application.tooling.ai.workflow.PageObjectExtensionProposal;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Compares an untrusted capability refusal with the trusted, fingerprint-bound Page Object contract. */
final class PageObjectCapabilityRefusalClassifier {
    private static final Pattern METHOD_REFERENCE = Pattern.compile(
            "(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)+[A-Za-z_$][A-Za-z0-9_$]*\\s*\\([^)]*\\)");
    private static final Pattern METHOD_SIGNATURE = Pattern.compile(
            "(?<![A-Za-z0-9_$])[A-Za-z_$][A-Za-z0-9_$]*\\s*\\([^)]*\\)");

    private PageObjectCapabilityRefusalClassifier() { }

    static Assessment classify(Object payload, AgentContextPack context, String currentFingerprint) {
        PageObjectCapabilityMissing missing = missing(payload);
        AgentContextPack.PageObjectCapabilityManifest manifest = context.pageObjectCapabilities();
        boolean fingerprintMatched = currentFingerprint != null && !currentFingerprint.isBlank()
                && Objects.equals(currentFingerprint, manifest.sourceFingerprint());
        if (manifest.completeness() != AgentContextPack.ManifestCompleteness.COMPLETE) {
            return new Assessment(PageObjectCapabilityException.RefusalCategory.CONTEXT_INCOMPLETE,
                    null, fingerprintMatched);
        }
        if (!fingerprintMatched) {
            return new Assessment(PageObjectCapabilityException.RefusalCategory.CONTEXT_INCOMPLETE,
                    null, false);
        }
        AgentContextPack.PageObjectCapability owner = manifest.classes().stream()
                .filter(value -> value.pageId().equals(missing.pageId()))
                .findFirst().orElse(null);
        if (owner == null) {
            return new Assessment(PageObjectCapabilityException.RefusalCategory.UNKNOWN, null, true);
        }
        String capability = bounded(missing.capability());
        String compact = capability.replaceAll("\\s+", "");
        String lower = capability.toLowerCase(Locale.ROOT);
        boolean constructible = owner.constructors().stream().anyMatch(PageObjectCapabilityRefusalClassifier::supported);
        boolean browserBound = owner.constructors().stream().anyMatch(PageObjectCapabilityRefusalClassifier::browserBound);
        List<AgentContextPack.MethodCapability> mentioned = owner.methods().stream()
                .filter(method -> mentions(capability, compact, owner, method)).toList();
        if (!mentioned.isEmpty()) {
            if (lower.contains("assert") && mentioned.stream().noneMatch(PageObjectCapabilityRefusalClassifier::booleanResult)) {
                return assessment(PageObjectCapabilityException.RefusalCategory.ASSERTION_INCOMPATIBLE, owner);
            }
            return assessment(PageObjectCapabilityException.RefusalCategory.CAPABILITY_PRESENT_BUT_REJECTED, owner);
        }
        if (browserWords(lower)) {
            return assessment(browserBound
                    ? PageObjectCapabilityException.RefusalCategory.CAPABILITY_PRESENT_BUT_REJECTED
                    : PageObjectCapabilityException.RefusalCategory.BROWSER_SESSION_BINDING_MISSING, owner);
        }
        if (constructionWords(lower)) {
            return assessment(constructible
                    ? PageObjectCapabilityException.RefusalCategory.CAPABILITY_PRESENT_BUT_REJECTED
                    : PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_CONSTRUCTION_MISSING, owner);
        }
        if (METHOD_REFERENCE.matcher(capability).find() || lower.contains("method") || lower.contains("operation")) {
            return assessment(PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_METHOD_MISSING, owner);
        }
        if (lower.contains("assert") || lower.contains("expected result")) {
            return assessment(owner.methods().stream().anyMatch(PageObjectCapabilityRefusalClassifier::booleanResult)
                    ? PageObjectCapabilityException.RefusalCategory.CAPABILITY_PRESENT_BUT_REJECTED
                    : PageObjectCapabilityException.RefusalCategory.ASSERTION_INCOMPATIBLE, owner);
        }
        if (lower.contains("credential") || lower.contains("test data") || lower.contains("username")
                || lower.contains("input value")) {
            return assessment(PageObjectCapabilityException.RefusalCategory.TEST_DATA_MISSING, owner);
        }
        if (lower.contains("navigate") || lower.contains("navigation") || lower.contains("setup")
                || lower.contains("precondition")) {
            return assessment(PageObjectCapabilityException.RefusalCategory.TEST_SETUP_MISSING, owner);
        }
        return assessment(PageObjectCapabilityException.RefusalCategory.UNKNOWN, owner);
    }

    private static Assessment assessment(PageObjectCapabilityException.RefusalCategory category,
                                         AgentContextPack.PageObjectCapability owner) {
        return new Assessment(category, owner.pageId(), true);
    }

    private static boolean mentions(String capability, String compact,
                                    AgentContextPack.PageObjectCapability owner,
                                    AgentContextPack.MethodCapability method) {
        String signature = method.signature().replaceAll("\\s+", "");
        if (capability.equals(method.name()) || compact.equals(signature)
                || compact.equals((owner.qualifiedName() + "." + method.signature()).replaceAll("\\s+", ""))) {
            return true;
        }
        if (METHOD_SIGNATURE.matcher(capability).find()) {
            return false;
        }
        return Pattern.compile("(?<![A-Za-z0-9_$])" + Pattern.quote(method.name()) + "(?![A-Za-z0-9_$])")
                .matcher(capability).find();
    }

    private static boolean supported(AgentContextPack.ConstructorCapability constructor) {
        return "PUBLIC".equals(constructor.accessibility()) && "DIRECT_CONSTRUCTION".equals(constructor.acquisition())
                && (constructor.parameters().isEmpty() || browserBound(constructor));
    }

    private static boolean browserBound(AgentContextPack.ConstructorCapability constructor) {
        return "PUBLIC".equals(constructor.accessibility()) && "DIRECT_CONSTRUCTION".equals(constructor.acquisition())
                && constructor.parameters().size() == 1
                && "org.openqa.selenium.WebDriver".equals(constructor.parameters().get(0).type());
    }

    private static boolean booleanResult(AgentContextPack.MethodCapability method) {
        return "boolean".equals(method.returnType()) || "Boolean".equals(method.returnType())
                || "java.lang.Boolean".equals(method.returnType());
    }

    private static boolean browserWords(String value) {
        return value.contains("webdriver") || value.contains("browser session") || value.contains("current driver")
                || value.contains("driver binding");
    }

    private static boolean constructionWords(String value) {
        return value.contains("construct") || value.contains("constructor") || value.contains("instantiate")
                || value.contains("initialization");
    }

    private static String bounded(String value) {
        String normalized = Objects.toString(value, "").replaceAll("[\\r\\n]+", " ");
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }

    private static PageObjectCapabilityMissing missing(Object payload) {
        if (payload instanceof PageObjectCapabilityMissing direct) return direct;
        if (payload instanceof PageObjectExtensionProposal extension) return extension.missing();
        throw new IllegalArgumentException("A capability alternative is required");
    }

    record Assessment(PageObjectCapabilityException.RefusalCategory category, String verifiedPageId,
                      boolean sourceFingerprintMatched) { }
}
