package utils.jsExecHelper;

import tests.fe.utils.contentassertions.ContentIssueCollector;
import utils.datetime.LocalDateTimeUtils;
import utils.jsExecHelper.actions.AssertActions;
import utils.jsExecHelper.react.ReactSafeExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Klej pomiędzy JsOverlayDebug (overlay asercje) a ContentIssueCollector.
 *
 * - wszystkie asercje nadal idą przez overlay (highlight, badge, HUD),
 * - dodatkowo każde FAIL trafia do ContentIssueCollector.
 */
public class OverlayContentAssertions {

    private final JsOverlayDebug overlay;
    private final ContentIssueCollector contentCollector;
    private final LocalDateTimeUtils ldt = new LocalDateTimeUtils();

    public OverlayContentAssertions(JsOverlayDebug overlay,
                                    ContentIssueCollector contentCollector) {
        this.overlay = overlay;
        this.contentCollector = contentCollector;
    }

    /**
     * Wrapper na JsOverlayDebug.assertGroup:
     * - wykonuje overlayowe asercje,
     * - zbiera summary,
     * - dla każdej porażki dopisuje wpis do ContentIssueCollector.
     */
    public JsOverlayDebug.AssertionSummary assertGroupWithContent(
            String groupName,
            Consumer<JsOverlayDebug.SoftAssertions> consumer,
            boolean failTestOnErrors
    ) {
        JsOverlayDebug.AssertionSummary summary =
                overlay.assertGroup(groupName, consumer, failTestOnErrors);

        pushFailuresToContentCollector(summary);

        return summary;
    }

    /**
     * To samo, ale dla wersji ReactSafe.
     */
    public JsOverlayDebug.AssertionSummary assertGroupReactSafeWithContent(
            String groupName,
            ReactSafeExecutor reactSafeExecutor,
            Consumer<JsOverlayDebug.SoftAssertions> consumer,
            boolean failTestOnErrors
    ) {
        JsOverlayDebug.AssertionSummary summary =
                overlay.assertGroupReactSafe(groupName, reactSafeExecutor, consumer, failTestOnErrors);

        pushFailuresToContentCollector(summary);

        return summary;
    }

    private void pushFailuresToContentCollector(JsOverlayDebug.AssertionSummary summary) {
        if (contentCollector == null) {
            return;
        }

        List<AssertActions.OverlayAssertionResult> failures = summary.getFailuresObjects();
        if (failures.isEmpty()) {
            return;
        }

        LocalDateTime now = ldt.generateLocalDateTimeInWarsaw();

        for (AssertActions.OverlayAssertionResult result : failures) {
            // Nie mamy osobno pola "expected"/"actual", ale mamy toMessage()
            String message = result.toMessage(); // np. "ASSERT TEXT_EQUALS [label] expected='X' actual='Y'"

            String location = "OVERLAY_ASSERT(" + summary.getGroupName() + ")";
            String expected = "Assertion should pass";
            String actual = message;
            String exceptionId = UUID.randomUUID().toString();

            contentCollector.addIssue(location, expected, actual, now, exceptionId);
        }
    }
}
