package io.github.testlens.application.tooling.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded deterministic JSON support shared by application tooling artifacts. @since 0.5.0 */
public final class StrictJson {
    public static final int MAX_DOCUMENT_BYTES = 16 * 1024 * 1024;
    private static final int MAX_COLLECTION_ITEMS = 100_000;
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxDocumentLength(MAX_DOCUMENT_BYTES).maxNestingDepth(48)
                    .maxStringLength(1_048_576).maxNameLength(256)
                    .maxNumberLength(64).maxTokenCount(2_000_000).build()).build();

    private StrictJson() {}

    public static byte[] write(Object value) {
        try {
            BoundedOutput out = new BoundedOutput(MAX_DOCUMENT_BYTES);
            try (JsonGenerator generator = FACTORY.createGenerator(ObjectWriteContext.empty(), out)) {
                writeValue(generator, value);
                generator.writeRaw('\n');
            }
            return out.toByteArray();
        } catch (IOException failure) {
            throw new JsonFormatException("JSON serialization failed", failure);
        }
    }

    public static Map<String, Object> readObject(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_DOCUMENT_BYTES) {
            throw new JsonFormatException("JSON document size is invalid");
        }
        try (JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(bytes))) {
            if (parser.nextToken() != JsonToken.START_OBJECT) throw new JsonFormatException("JSON root must be an object");
            @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) readValue(parser, 0);
            if (parser.nextToken() != null) throw new JsonFormatException("Trailing JSON content");
            return result;
        } catch (JsonFormatException failure) {
            throw failure;
        } catch (IOException | RuntimeException failure) {
            throw new JsonFormatException("Malformed JSON", failure);
        }
    }

    private static Object readValue(JsonParser parser, int depth) throws IOException {
        if (depth > 48) throw new JsonFormatException("JSON nesting exceeds limit");
        return switch (parser.currentToken()) {
            case START_OBJECT -> {
                Map<String, Object> values = new LinkedHashMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String name = parser.currentName();
                    parser.nextToken();
                    values.put(name, readValue(parser, depth + 1));
                    if (values.size() > MAX_COLLECTION_ITEMS) throw new JsonFormatException("JSON object exceeds item limit");
                }
                yield Collections.unmodifiableMap(new LinkedHashMap<>(values));
            }
            case START_ARRAY -> {
                List<Object> values = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (values.size() >= MAX_COLLECTION_ITEMS) throw new JsonFormatException("JSON array exceeds item limit");
                    values.add(readValue(parser, depth + 1));
                }
                yield Collections.unmodifiableList(new ArrayList<>(values));
            }
            case VALUE_STRING -> parser.getString();
            case VALUE_NUMBER_INT -> parser.getLongValue();
            case VALUE_NUMBER_FLOAT -> parser.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            default -> throw new JsonFormatException("Unexpected JSON token " + parser.currentToken());
        };
    }

    private static void writeValue(JsonGenerator generator, Object value) throws IOException {
        if (value == null) { generator.writeNull(); return; }
        if (value instanceof String string) { generator.writeString(string); return; }
        if (value instanceof Integer number) { generator.writeNumber(number); return; }
        if (value instanceof Long number) { generator.writeNumber(number); return; }
        if (value instanceof Double number) { generator.writeNumber(number); return; }
        if (value instanceof Boolean bool) { generator.writeBoolean(bool); return; }
        if (value instanceof Instant instant) { generator.writeString(instant.toString()); return; }
        if (value instanceof Enum<?> enumeration) { generator.writeString(enumeration.name()); return; }
        if (value instanceof Map<?, ?> map) {
            generator.writeStartObject();
            for (Map.Entry<?, ?> entry : map.entrySet().stream()
                    .sorted(Comparator.comparing(item -> String.valueOf(item.getKey()))).toList()) {
                generator.writeName(String.valueOf(entry.getKey()));
                writeValue(generator, entry.getValue());
            }
            generator.writeEndObject(); return;
        }
        if (value instanceof Collection<?> collection) {
            generator.writeStartArray();
            for (Object item : collection) writeValue(generator, item);
            generator.writeEndArray(); return;
        }
        if (value.getClass().isRecord()) {
            generator.writeStartObject();
            for (var component : value.getClass().getRecordComponents()) {
                try {
                    generator.writeName(component.getName());
                    writeValue(generator, component.getAccessor().invoke(value));
                } catch (IllegalAccessException | InvocationTargetException failure) {
                    throw new IOException("Cannot serialize record " + value.getClass().getName(), failure);
                }
            }
            generator.writeEndObject(); return;
        }
        throw new IOException("Unsupported JSON value " + value.getClass().getName());
    }

    private static final class BoundedOutput extends ByteArrayOutputStream {
        private final int max;
        private BoundedOutput(int max) { this.max = max; }
        @Override public synchronized void write(int value) { ensure(1); super.write(value); }
        @Override public synchronized void write(byte[] bytes, int offset, int length) { ensure(length); super.write(bytes, offset, length); }
        private void ensure(int amount) { if (count + amount > max) throw new JsonFormatException("JSON output exceeds limit"); }
    }

    public static final class JsonFormatException extends IllegalArgumentException {
        public JsonFormatException(String message) { super(message); }
        public JsonFormatException(String message, Throwable cause) { super(message, cause); }
    }
}
