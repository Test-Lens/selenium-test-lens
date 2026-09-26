package io.github.testlens.selector.tooling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static io.github.testlens.selector.tooling.SelectorIndexModel.*;

final class SelectorIndexJson {
    private SelectorIndexJson() {
    }

    static String serialize(SelectorIndex index) {
        Json out = new Json();
        out.object(() -> {
            out.number("schemaVersion", SCHEMA_VERSION);
            out.name("generator").object(() -> {
                out.string("name", GENERATOR_NAME);
                out.string("version", GENERATOR_VERSION);
            });
            out.name("project").object(() -> project(out, index.project()));
            out.name("issues").array(index.issues(), issue -> issue(out, issue));
            out.name("files").array(index.files(), file -> file(out, file));
            out.name("coverage").object(() -> coverage(out, index.coverage()));
        });
        return out.value();
    }

    static void writeLocal(SelectorIndex index, Path projectRoot, Path output) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize().toRealPath();
        Path target = root.resolve("target").normalize();
        Path destination = output.isAbsolute() ? output.normalize() : root.resolve(output).normalize();
        if (!destination.startsWith(target)) {
            throw new IllegalArgumentException("Selector index output must be under project target/: " + output);
        }
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "selector-index-", ".tmp");
        try {
            Files.writeString(temporary, serialize(index), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void project(Json out, ProjectMetadata project) {
        if (project.sourceRevision() != null) out.string("sourceRevision", project.sourceRevision());
        out.name("sourceRoots").array(project.sourceRoots(), out::string);
        out.string("parserVersion", project.parserVersion());
        out.string("languageLevel", project.languageLevel());
        out.string("encoding", project.encoding());
        out.string("toolConfigFingerprint", project.toolConfigFingerprint());
        out.string("classpathFingerprint", project.classpathFingerprint());
    }

    private static void file(Json out, SourceFileIndex file) {
        out.object(() -> {
            out.string("logicalPath", file.logicalPath());
            out.string("sourceLanguage", file.sourceLanguage());
            out.string("encoding", file.encoding());
            if (file.contentHash() != null) out.string("contentHash", file.contentHash());
            out.bool("generated", file.generated());
            out.bool("readOnly", file.readOnly());
            out.string("parseStatus", file.parseStatus().name());
            out.name("issues").array(file.issues(), issue -> issue(out, issue));
            out.name("declarations").array(file.declarations(), declaration -> declaration(out, declaration));
        });
    }

    private static void declaration(Json out, DeclarationRecord declaration) {
        out.object(() -> {
            out.number("schemaVersion", declaration.schemaVersion());
            out.string("declarationRef", declaration.declarationRef());
            out.string("sourceLanguage", declaration.sourceLanguage());
            out.string("logicalPath", declaration.logicalPath());
            out.name("sourceRange").object(() -> range(out, declaration.sourceRange()));
            out.name("declaringSymbol").object(() -> symbol(out, declaration.declaringSymbol()));
            out.string("declarationKind", declaration.declarationKind().name());
            out.name("locatorExpression").object(() -> expression(out, declaration.locatorExpression()));
            if (declaration.resolvedLocator() != null) {
                out.name("resolvedLocator").object(() -> {
                    out.string("strategy", declaration.resolvedLocator().strategy());
                    out.string("value", declaration.resolvedLocator().value());
                });
            }
            out.string("resolutionStatus", declaration.resolutionStatus().name());
            out.bool("generated", declaration.generated());
            out.bool("readOnly", declaration.readOnly());
            out.string("contentFingerprint", declaration.contentFingerprint());
        });
    }

    private static void range(Json out, SourceRange range) {
        out.number("startLine", range.startLine());
        out.number("startColumn", range.startColumn());
        out.number("endLine", range.endLine());
        out.number("endColumn", range.endColumn());
        out.number("startOffset", range.startOffset());
        out.number("endOffsetExclusive", range.endOffsetExclusive());
        out.string("offsetUnit", "UTF16_CODE_UNIT");
    }

    private static void symbol(Json out, DeclaringSymbol symbol) {
        out.string("kind", symbol.kind());
        if (symbol.qualifiedTypeName() != null) out.string("qualifiedTypeName", symbol.qualifiedTypeName());
        if (symbol.memberSignature() != null) out.string("memberSignature", symbol.memberSignature());
        if (symbol.localScopeFingerprint() != null) out.string("localScopeFingerprint", symbol.localScopeFingerprint());
    }

    private static void expression(Json out, LocatorExpression expression) {
        out.string("kind", expression.kind().name());
        out.string("normalizedExpression", expression.normalizedExpression());
        if (expression.factorySymbol() != null) out.string("factorySymbol", expression.factorySymbol());
        out.name("parameterDependencies").array(expression.parameterDependencies(), out::string);
        out.name("children").array(expression.children(), child -> out.object(() -> {
            expression(out, child.expression());
            if (child.resolvedLocator() != null) {
                out.name("resolvedLocator").object(() -> {
                    out.string("strategy", child.resolvedLocator().strategy());
                    out.string("value", child.resolvedLocator().value());
                });
            }
            out.string("resolutionStatus", child.resolutionStatus().name());
        }));
    }

    private static void issue(Json out, Issue issue) {
        out.object(() -> {
            out.string("code", issue.code());
            out.string("message", issue.message());
            if (issue.logicalPath() != null) out.string("logicalPath", issue.logicalPath());
        });
    }

    private static void coverage(Json out, Coverage coverage) {
        out.number("sourceRootsRequested", coverage.sourceRootsRequested());
        out.number("sourceRootsFound", coverage.sourceRootsFound());
        out.number("filesDiscovered", coverage.filesDiscovered());
        out.number("filesParsed", coverage.filesParsed());
        out.number("filesFailed", coverage.filesFailed());
        out.number("filesExcluded", coverage.filesExcluded());
        out.number("generatedFilesExcluded", coverage.generatedFilesExcluded());
        out.number("unsupportedLanguageFiles", coverage.unsupportedLanguageFiles());
        out.number("declarationsFound", coverage.declarationsFound());
        out.number("resolved", coverage.resolved());
        out.number("partiallyResolved", coverage.partiallyResolved());
        out.number("dynamic", coverage.dynamic());
        out.number("custom", coverage.custom());
        out.number("unsupported", coverage.unsupported());
        out.number("errors", coverage.errors());
        out.bool("incompleteClasspath", coverage.incompleteClasspath());
        out.number("symbolResolutionIssues", coverage.symbolResolutionIssues());
    }

    private static final class Json {
        private final StringBuilder out = new StringBuilder();
        private boolean first = true;

        String value() {
            return out.toString();
        }

        Json name(String name) {
            separator();
            quote(name);
            out.append(':');
            return this;
        }

        void object(Runnable body) {
            out.append('{');
            boolean outer = first;
            first = true;
            body.run();
            out.append('}');
            first = outer;
        }

        <T> void array(List<T> values, java.util.function.Consumer<T> writer) {
            out.append('[');
            boolean outer = first;
            first = true;
            for (T value : values) {
                separator();
                writer.accept(value);
            }
            out.append(']');
            first = outer;
        }

        void string(String name, String value) {
            name(name);
            quote(value);
        }

        void string(String value) {
            quote(value);
        }

        void number(String name, long value) {
            name(name);
            out.append(value);
        }

        void bool(String name, boolean value) {
            name(name);
            out.append(value);
        }

        private void separator() {
            if (!first) out.append(',');
            first = false;
        }

        private void quote(String value) {
            out.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    case '\b' -> out.append("\\b");
                    case '\f' -> out.append("\\f");
                    case '\n' -> out.append("\\n");
                    case '\r' -> out.append("\\r");
                    case '\t' -> out.append("\\t");
                    default -> {
                        if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                        else out.append(c);
                    }
                }
            }
            out.append('"');
        }
    }
}
