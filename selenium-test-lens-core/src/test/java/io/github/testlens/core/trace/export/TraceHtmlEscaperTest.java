package io.github.testlens.core.trace.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraceHtmlEscaperTest {

    @Test
    void escapesHtmlSensitiveCharacters() {
        String escaped = TraceHtmlEscaper.escape("<script>alert('x') & \"quote\"</script>");

        assertEquals("&lt;script&gt;alert(&#39;x&#39;) &amp; &quot;quote&quot;&lt;/script&gt;", escaped);
    }

    @Test
    void nullEscapesToEmptyString() {
        assertEquals("", TraceHtmlEscaper.escape(null));
    }
}
