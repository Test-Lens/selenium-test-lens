package io.github.testlens.application.tooling.json;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ToolingFixtures;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationModelJsonTest {
    private final ApplicationModelJson codec = new ApplicationModelJson();

    @Test
    void fullRoundTripProducesCanonicalStableBytes() {
        ApplicationModel model = ToolingFixtures.model();
        byte[] first = codec.write(model);
        ApplicationModel restored = codec.read(first);

        assertEquals(model, restored);
        assertArrayEquals(first, codec.write(restored));
    }

    @Test
    void rejectsDuplicateUnknownOversizeAndUnsupportedSchema() {
        String valid = new String(codec.write(ToolingFixtures.model()), StandardCharsets.UTF_8);
        assertThrows(StrictJson.JsonFormatException.class,
                () -> codec.read(valid.replaceFirst("\\{", "{\"applicationId\":\"duplicate\",").getBytes(StandardCharsets.UTF_8)));
        assertThrows(StrictJson.JsonFormatException.class,
                () -> codec.read(valid.replaceFirst("\\{", "{\"unexpected\":true,").getBytes(StandardCharsets.UTF_8)));
        assertThrows(StrictJson.JsonFormatException.class,
                () -> codec.read(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":99").getBytes(StandardCharsets.UTF_8)));
        assertThrows(StrictJson.JsonFormatException.class,
                () -> codec.read(new byte[StrictJson.MAX_DOCUMENT_BYTES + 1]));
    }
}
