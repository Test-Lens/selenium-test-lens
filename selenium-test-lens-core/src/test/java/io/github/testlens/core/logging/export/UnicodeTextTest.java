package io.github.testlens.core.logging.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeTextTest {

    @Test
    void preservesUnicodeClustersAtTheFieldLimit() {
        assertEquals("A...", UnicodeText.truncate("A😀B", 2));
        assertEquals("A...", UnicodeText.truncate("A❤️B", 2));
        assertEquals("A...", UnicodeText.truncate("A👩‍💻B", 4));
        assertEquals("A...", UnicodeText.truncate("A👨‍👩‍👧‍👦B", 7));
        assertEquals("A...", UnicodeText.truncate("A🇵🇱B", 3));
        assertEquals("A...", UnicodeText.truncate("A👍🏽B", 3));
        assertEquals("A...", UnicodeText.truncate("A1️⃣B", 3));
        assertEquals("A...", UnicodeText.truncate("AéB", 2));
    }

    @Test
    void includesAWholeClusterWhenItFits() {
        assertEquals("A👩‍💻...", UnicodeText.truncate("A👩‍💻B", 6));
        assertEquals("A🇵🇱...", UnicodeText.truncate("A🇵🇱B", 5));
    }
}
