package io.github.testlens.application.tooling.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Canonical, manifest-bound identity of a callable Page Object method. @since 0.5.0 */
public record CanonicalPageObjectApiReference(String owner, String method, List<String> parameterTypes) {
    public CanonicalPageObjectApiReference {
        if (!qualifiedIdentifier(owner) || !identifier(method)) {
            throw new IllegalArgumentException("A qualified owner and Java method name are required");
        }
        parameterTypes = (parameterTypes == null ? List.<String>of() : parameterTypes).stream()
                .map(CanonicalPageObjectApiReference::canonicalType)
                .toList();
        if (parameterTypes.stream().anyMatch(value -> !TypeParser.valid(value))) {
            throw new IllegalArgumentException("A parameter type is invalid");
        }
    }

    public String render() {
        return owner + "#" + method + "(" + String.join(",", parameterTypes) + ")";
    }

    /** Parses only the canonical representation emitted by {@link #render()}. */
    public static CanonicalPageObjectApiReference parse(String value) {
        RawReference raw = RawReference.parse(value);
        if (raw == null || raw.separator() != '#' || !qualifiedIdentifier(raw.owner())) {
            throw new IllegalArgumentException("The Page Object API reference is not canonical");
        }
        CanonicalPageObjectApiReference result = new CanonicalPageObjectApiReference(
                raw.owner(), raw.method(), raw.parameterTypes());
        if (!result.render().equals(value)) {
            throw new IllegalArgumentException("The Page Object API reference is not canonical");
        }
        return result;
    }

    public static CanonicalPageObjectApiReference of(String callableOwner,
                                                       AgentContextPack.MethodCapability method) {
        return of(callableOwner, method.name(),
                method.parameters().stream().map(AgentContextPack.CapabilityParameter::type).toList());
    }

    public static CanonicalPageObjectApiReference of(String callableOwner, String method,
                                                       List<String> parameterTypes) {
        return new CanonicalPageObjectApiReference(callableOwner, method, parameterTypes);
    }

    public static List<CanonicalPageObjectApiReference> allowed(
            AgentContextPack.PageObjectCapabilityManifest manifest) {
        if (manifest == null) return List.of();
        return manifest.classes().stream()
                .flatMap(owner -> owner.methods().stream().map(method -> of(owner.qualifiedName(), method)))
                .distinct().sorted(Comparator.comparing(CanonicalPageObjectApiReference::render)).toList();
    }

    /** Resolves a canonical or explicitly supported equivalent spelling against a trusted manifest. */
    public static Resolution resolve(String value, AgentContextPack.PageObjectCapabilityManifest manifest) {
        if (value == null || value.isEmpty() || !value.equals(value.strip())) {
            return Resolution.fail(Category.INVALID_REFERENCE_STRUCTURE);
        }
        if (value.contains("::")) return Resolution.fail(Category.METHOD_SEPARATOR_MISMATCH);
        RawReference raw = RawReference.parse(value);
        if (raw == null) return Resolution.fail(looksLikeTypedReference(value)
                ? Category.PARAMETER_TYPE_FORMAT : Category.INVALID_REFERENCE_STRUCTURE);
        if (manifest == null || manifest.classes().isEmpty()) return Resolution.fail(Category.UNTRUSTED_REFERENCE);

        List<OwnerMethod> named = new ArrayList<>();
        for (AgentContextPack.PageObjectCapability owner : manifest.classes()) {
            boolean callableOwner = owner.qualifiedName().equals(raw.owner())
                    || simpleName(owner.qualifiedName()).equals(raw.owner());
            for (AgentContextPack.MethodCapability candidate : owner.methods()) {
                boolean declaringOwner = candidate.declaringType() != null
                        && (candidate.declaringType().equals(raw.owner())
                        || simpleName(candidate.declaringType()).equals(raw.owner()));
                if ((callableOwner || declaringOwner) && candidate.name().equals(raw.method())) {
                    named.add(new OwnerMethod(owner, candidate));
                }
            }
        }
        if (named.isEmpty()) {
            boolean knownOwner = manifest.classes().stream().anyMatch(owner -> owner.qualifiedName().equals(raw.owner())
                    || simpleName(owner.qualifiedName()).equals(raw.owner()));
            return Resolution.fail(knownOwner ? Category.METHOD_MISSING : Category.DECLARING_CLASS_MISMATCH);
        }
        List<OwnerMethod> sameArity = named.stream()
                .filter(candidate -> candidate.method().parameters().size() == raw.parameterTypes().size()).toList();
        if (sameArity.isEmpty()) return Resolution.fail(Category.ARITY_MISMATCH);

        List<OwnerMethod> exact = sameArity.stream().filter(candidate -> exactTypes(raw, candidate.method())).toList();
        List<OwnerMethod> matched = exact.isEmpty()
                ? sameArity.stream().filter(candidate -> equivalentTypes(raw, candidate.method())).toList()
                : exact;
        if (matched.isEmpty()) return Resolution.fail(Category.METHOD_MISSING);
        List<CanonicalPageObjectApiReference> resolved = matched.stream()
                .map(candidate -> of(candidate.owner().qualifiedName(), candidate.method())).distinct().toList();
        if (resolved.size() != 1) return Resolution.fail(Category.AMBIGUOUS_OVERLOAD);
        return Resolution.valid(resolved.get(0));
    }

    private static boolean exactTypes(RawReference raw, AgentContextPack.MethodCapability method) {
        for (int index = 0; index < raw.parameterTypes().size(); index++) {
            if (!canonicalType(raw.parameterTypes().get(index))
                    .equals(canonicalType(method.parameters().get(index).type()))) return false;
        }
        return true;
    }

    private static boolean equivalentTypes(RawReference raw, AgentContextPack.MethodCapability method) {
        for (int index = 0; index < raw.parameterTypes().size(); index++) {
            if (!simpleTypeKey(raw.parameterTypes().get(index))
                    .equals(simpleTypeKey(method.parameters().get(index).type()))) return false;
        }
        return true;
    }

    private static String simpleTypeKey(String value) {
        String canonical = canonicalType(value);
        StringBuilder result = new StringBuilder(canonical.length());
        for (int offset = 0; offset < canonical.length();) {
            char current = canonical.charAt(offset);
            if (!Character.isJavaIdentifierStart(current)) {
                result.append(current);
                offset++;
                continue;
            }
            int end = offset + 1;
            while (end < canonical.length() && (Character.isJavaIdentifierPart(canonical.charAt(end))
                    || canonical.charAt(end) == '.')) end++;
            String token = canonical.substring(offset, end);
            if (result.length() > 0 && result.charAt(result.length() - 1) == '?' && token.startsWith("extends")) {
                result.append("extends").append(simpleName(token.substring("extends".length())));
            } else if (result.length() > 0 && result.charAt(result.length() - 1) == '?' && token.startsWith("super")) {
                result.append("super").append(simpleName(token.substring("super".length())));
            } else if ("extends".equals(token) || "super".equals(token)) result.append(token);
            else result.append(simpleName(token));
            offset = end;
        }
        return result.toString();
    }

    private static String canonicalType(String value) {
        String normalized = Objects.toString(value, "").strip()
                .replaceAll("\\?\\s+extends\\s+", "?extends")
                .replaceAll("\\?\\s+super\\s+", "?super");
        return normalized.chars().anyMatch(Character::isWhitespace) ? "" : normalized;
    }

    private static boolean looksLikeTypedReference(String value) {
        int open = value.indexOf('(');
        if (open <= 0 || value.charAt(value.length() - 1) != ')') return false;
        String head = value.substring(0, open);
        int separator = Math.max(head.lastIndexOf('#'), head.lastIndexOf('.'));
        return separator > 0 && separator < head.length() - 1
                && ownerIdentifier(head.substring(0, separator))
                && identifier(head.substring(separator + 1));
    }

    private static String simpleName(String value) {
        return value.substring(value.lastIndexOf('.') + 1);
    }

    private static boolean qualifiedIdentifier(String value) {
        if (value == null) return false;
        String[] parts = value.split("\\.", -1);
        if (parts.length < 2) return false;
        for (String part : parts) if (!identifier(part)) return false;
        return true;
    }

    private static boolean ownerIdentifier(String value) {
        if (value == null) return false;
        for (String part : value.split("\\.", -1)) if (!identifier(part)) return false;
        return true;
    }

    private static boolean identifier(String value) {
        if (value == null || value.isEmpty() || !Character.isJavaIdentifierStart(value.charAt(0))) return false;
        for (int index = 1; index < value.length(); index++) {
            if (!Character.isJavaIdentifierPart(value.charAt(index))) return false;
        }
        return true;
    }

    public enum Category {
        VALID,
        INVALID_REFERENCE_STRUCTURE,
        METHOD_SEPARATOR_MISMATCH,
        PARAMETER_TYPE_FORMAT,
        METHOD_MISSING,
        ARITY_MISMATCH,
        AMBIGUOUS_OVERLOAD,
        DECLARING_CLASS_MISMATCH,
        UNTRUSTED_REFERENCE
    }

    public record Resolution(boolean valid, Category category, CanonicalPageObjectApiReference reference) {
        private static Resolution valid(CanonicalPageObjectApiReference reference) {
            return new Resolution(true, Category.VALID, reference);
        }
        private static Resolution fail(Category category) { return new Resolution(false, category, null); }
    }

    private record OwnerMethod(AgentContextPack.PageObjectCapability owner,
                               AgentContextPack.MethodCapability method) { }

    private record RawReference(String owner, char separator, String method, List<String> parameterTypes) {
        private static RawReference parse(String value) {
            if (value == null || value.isEmpty()) return null;
            int open = value.indexOf('(');
            if (open <= 0 || value.charAt(value.length() - 1) != ')' || value.indexOf(')', open) != value.length() - 1) {
                return null;
            }
            String head = value.substring(0, open);
            int hash = head.lastIndexOf('#');
            int dot = head.lastIndexOf('.');
            int separator = hash >= 0 ? hash : dot;
            if (separator <= 0 || separator == head.length() - 1) return null;
            String owner = head.substring(0, separator);
            String method = head.substring(separator + 1);
            if (!ownerIdentifier(owner) || !identifier(method)) return null;
            List<String> types = splitTypes(value.substring(open + 1, value.length() - 1));
            return types == null ? null : new RawReference(owner, head.charAt(separator), method, types);
        }

        private static List<String> splitTypes(String parameters) {
            if (parameters.isEmpty()) return List.of();
            List<String> values = new ArrayList<>();
            int depth = 0;
            int start = 0;
            for (int index = 0; index < parameters.length(); index++) {
                char current = parameters.charAt(index);
                if (current == '<') depth++;
                else if (current == '>') { if (--depth < 0) return null; }
                else if (current == ',' && depth == 0) {
                    values.add(parameters.substring(start, index));
                    start = index + 1;
                }
            }
            if (depth != 0) return null;
            values.add(parameters.substring(start));
            List<String> canonical = values.stream().map(CanonicalPageObjectApiReference::canonicalType).toList();
            if (canonical.stream().anyMatch(value -> !TypeParser.valid(value))) return null;
            return canonical;
        }
    }

    private static final class TypeParser {
        private final String value;
        private int offset;
        private TypeParser(String value) { this.value = value; }
        private static boolean valid(String value) {
            if (value.isEmpty()) return false;
            TypeParser parser = new TypeParser(value);
            return parser.type(true, true, false) && parser.offset == value.length();
        }
        private boolean type(boolean arrays, boolean varargs, boolean wildcard) {
            if (take('?')) {
                if (!wildcard) return false;
                if (takeWord("extends") || takeWord("super")) return type(true, false, false);
                return true;
            }
            if (!namedType()) return false;
            if (!arrays) return true;
            while (takeWord("[]")) { }
            if (varargs) takeWord("...");
            return true;
        }
        private boolean namedType() {
            if (!identifier()) return false;
            if (!typeArguments()) return false;
            while (offset + 1 < value.length() && value.charAt(offset) == '.'
                    && Character.isJavaIdentifierStart(value.charAt(offset + 1))) {
                offset++;
                if (!identifier() || !typeArguments()) return false;
            }
            return true;
        }
        private boolean typeArguments() {
            if (!take('<')) return true;
            if (!type(true, false, true)) return false;
            while (take(',')) if (!type(true, false, true)) return false;
            return take('>');
        }
        private boolean identifier() {
            if (offset >= value.length() || !Character.isJavaIdentifierStart(value.charAt(offset))) return false;
            offset++;
            while (offset < value.length() && Character.isJavaIdentifierPart(value.charAt(offset))) offset++;
            return true;
        }
        private boolean take(char expected) {
            if (offset < value.length() && value.charAt(offset) == expected) { offset++; return true; }
            return false;
        }
        private boolean takeWord(String expected) {
            if (!value.startsWith(expected, offset)) return false;
            offset += expected.length();
            return true;
        }
    }
}
