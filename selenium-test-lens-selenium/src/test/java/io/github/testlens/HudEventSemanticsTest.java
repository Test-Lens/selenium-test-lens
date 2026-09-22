package io.github.testlens;

import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEventSemanticsTest {
    @Test
    void classifiesFunctionalLifecyclesStructurally() {
        assertSemantic(UiTestLensEventType.ASSERTION_STARTED, UiTestLensStatus.STARTED, "ASSERTION", "RUNNING");
        assertSemantic(UiTestLensEventType.ASSERTION_PASSED, UiTestLensStatus.PASSED, "ASSERTION", "PASSED");
        assertSemantic(UiTestLensEventType.ASSERTION_RETRY, UiTestLensStatus.WARN, "ASSERTION", "RETRYING");
        assertSemantic(UiTestLensEventType.ASSERTION_FAILED, UiTestLensStatus.FAILED, "ASSERTION", "FAILED");
        assertSemantic(UiTestLensEventType.LOCATOR_ACTION_STARTED, UiTestLensStatus.STARTED, "ACTION", "RUNNING");
        assertSemantic(UiTestLensEventType.LOCATOR_ACTION_PASSED, UiTestLensStatus.PASSED, "ACTION", "PASSED");
        assertSemantic(UiTestLensEventType.LOCATOR_ACTION_FAILED, UiTestLensStatus.FAILED, "ACTION", "FAILED");
        assertSemantic(UiTestLensEventType.HUD, UiTestLensStatus.INFO, "USER", "INFO");
    }

    @Test
    void technicalSuccessIsDebugRatherThanFunctionalPass() {
        UiTestLensEventType[] types = {
                UiTestLensEventType.LOCATOR_RESOLVE_PASSED,
                UiTestLensEventType.ACTIONABILITY_CHECK_PASSED,
                UiTestLensEventType.HIGHLIGHT};
        String[] categories = {"LOCATOR", "ACTIONABILITY", "HIGHLIGHT"};
        for (int index = 0; index < types.length; index++) {
            HudEventSemantics semantics = HudEventSemantics.from(UiTestLensLogEntry.builder()
                    .eventType(types[index]).status(UiTestLensStatus.PASSED).build());
            assertEquals(categories[index], semantics.category());
            assertEquals("DEBUG", semantics.phase());
            assertTrue(semantics.technical());
        }
    }

    @Test
    void routineAssertionPollingIsNotPresentedAsAnOperationRetry() {
        HudEventSemantics semantics = HudEventSemantics.from(UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.ASSERTION_RETRY)
                .status(UiTestLensStatus.WARN)
                .metadata("retryKind", "poll")
                .build());
        assertEquals("ASSERTION", semantics.category());
        assertEquals("RUNNING", semantics.phase());
    }

    @Test
    void oldNeutralEntryHasSafeSystemInfoFallback() {
        HudEventSemantics semantics = HudEventSemantics.from(UiTestLensLogEntry.info("legacy"));
        assertEquals("SYSTEM", semantics.category());
        assertEquals("INFO", semantics.phase());
        assertEquals("", semantics.operationId());
        assertFalse(semantics.technical());
    }

    @Test
    void customIconAppliesOnlyToUserEventsAndBlankMeansDefault() {
        String compoundIcon = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        HudEventSemantics user = HudEventSemantics.from(UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.HUD)
                .metadata(HudEventSemantics.USER_ICON_METADATA, compoundIcon)
                .build());
        assertEquals("USER", user.category());
        assertEquals(compoundIcon, user.customIcon());
        assertEquals(compoundIcon, user.toBrowserMap("INFO").get("customIcon"));

        HudEventSemantics blank = HudEventSemantics.from(UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.HUD)
                .metadata(HudEventSemantics.USER_ICON_METADATA, "  \t")
                .build());
        assertEquals("", blank.customIcon());

        HudEventSemantics action = HudEventSemantics.from(UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.ACTION)
                .metadata(HudEventSemantics.USER_ICON_METADATA, compoundIcon)
                .build());
        assertEquals("ACTION", action.category());
        assertEquals("", action.customIcon());
    }

    private static void assertSemantic(UiTestLensEventType type, UiTestLensStatus status,
                                       String category, String phase) {
        HudEventSemantics semantics = HudEventSemantics.from(UiTestLensLogEntry.builder()
                .eventType(type).status(status).build());
        assertEquals(category, semantics.category());
        assertEquals(phase, semantics.phase());
    }
}
