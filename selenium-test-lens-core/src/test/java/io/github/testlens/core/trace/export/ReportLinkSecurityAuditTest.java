package io.github.testlens.core.trace.export;

import io.github.testlens.core.trace.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReportLinkSecurityAuditTest {
    @Test void executableArtifactUrlsRemainText() {
        var session = UiTestLensSession.start("link audit");
        session.attachUrl("script", TraceArtifactType.CUSTOM_URL, "javascript:alert(1)");
        session.attachUrl("data", TraceArtifactType.CUSTOM_URL, "data:text/html,<script>alert(1)</script>");
        var html = session.exportHtml();
        assertFalse(html.contains("href=\"javascript:"));
        assertFalse(html.contains("href=\"data:"));
        assertTrue(html.contains("javascript:"));
    }
    @Test void webArtifactUrlsStayClickableAndEscaped() {
        var session = UiTestLensSession.start("web links");
        session.attachUrl("web", TraceArtifactType.CUSTOM_URL, "https://example.test/report?a=1&b=2");
        assertTrue(session.exportHtml().contains("href=\"https://example.test/report?a=1&amp;b=2\""));
    }
    @Test void executableFailureArchiveRemainsText() {
        var session = UiTestLensSession.start("archive link");
        session.addEvent(TraceEvent.builder(TraceEventType.FAILURE_BUNDLE, TraceStatus.INFO, "bundle")
                .attribute("archive", "javascript:alert(1)").build());
        assertFalse(session.exportHtml().contains("href=\"javascript:"));
    }
    @Test void embeddedAsciiWhitespaceDoesNotBypassSchemeCheck() {
        var session = UiTestLensSession.start("scheme parsing", RetryOutcomePolicy.REPORT_ONLY, 0,
                io.github.testlens.core.redaction.RedactionPolicy.disabled());
        for (String url : new String[] {"JaVaScRiPt:alert(1)", "java\tscript:alert(1)", "\njavascript:alert(1)", "vbscript:msgbox(1)"}) {
            session.attachUrl("unsafe", TraceArtifactType.CUSTOM_URL, url);
        }
        assertFalse(session.exportHtml().contains("href=\"JaVaScRiPt:"));
        assertFalse(session.exportHtml().contains("href=\"java\tscript:"));
        assertFalse(session.exportHtml().contains("href=\"\njavascript:"));
        assertFalse(session.exportHtml().contains("href=\"vbscript:"));
    }
}
