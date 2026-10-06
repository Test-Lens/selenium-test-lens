package io.github.testlens.application.tooling.ai.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowReportTest {
    @Test void rendersDeterministicStructuredAndHumanSuccessReport() {
        WorkflowReport report = report(TestEngineeringRun.State.SUCCESS, List.of(
                step(3, "REVIEW", WorkflowReport.Status.APPROVE, "no policy violations"),
                step(1, "CONTEXT", WorkflowReport.Status.PASS, "one Page Object"),
                step(2, "EXECUTION", WorkflowReport.Status.PASS, "targeted browser test")));

        String json = report.jsonText();
        assertTrue(json.contains("\"schemaVersion\":1"));
        assertTrue(json.indexOf("CONTEXT") < json.indexOf("EXECUTION"));
        assertTrue(json.contains("\"finalState\":\"SUCCESS\""));
        assertTrue(report.text().contains("Context: 7700 bytes"));
        assertTrue(report.text().contains("external agents: 1200 ms"));
    }

    @Test void reportsFailureAndReviewOnlyRepairWithoutClaimingApply() {
        WorkflowReport report = report(TestEngineeringRun.State.NEEDS_HUMAN_REVIEW, List.of(
                step(1, "EXECUTION", WorkflowReport.Status.FAIL, "real NoSuchElementException"),
                step(2, "CLASSIFICATION", WorkflowReport.Status.PASS, "SELECTOR_INSTABILITY"),
                step(3, "REPAIR", WorkflowReport.Status.PROPOSE_ONLY, "trusted apply not performed")));

        assertTrue(report.jsonText().contains("PROPOSE_ONLY"));
        assertTrue(report.text().contains("Result: NEEDS_HUMAN_REVIEW"));
        assertFalse(report.text().contains("APPLIED"));
    }

    private static WorkflowReport report(TestEngineeringRun.State state, List<WorkflowReport.Step> steps) {
        TestEngineeringRun run = new TestEngineeringRun("run-report", request(), state, List.of(),
                new TestEngineeringRun.Metrics(0, 0, 0, 3, 1, 1), List.of(), null);
        return WorkflowReport.from(run, steps, new WorkflowReport.Metrics(
                7_700, 8_100, 2_300, 1_200, 60, 20, 700, 1_980, 3, 1, 1, 0, 1), List.of());
    }

    private static WorkflowReport.Step step(int order, String name, WorkflowReport.Status status, String summary) {
        return new WorkflowReport.Step(order, name, status, 10, 0, 0, summary, List.of());
    }

    private static TestEngineeringRequest request() {
        return new TestEngineeringRequest(
                new TestEngineeringRequest.Requirement("Invalid password shows an error", List.of("error visible")),
                new TestEngineeringRequest.Scope(List.of("login"), List.of()),
                new TestEngineeringRequest.Framework("JUnit", "5", "junit-jupiter"),
                new TestEngineeringRequest.Target("fixture", "InvalidPasswordTest", "invalid-password"),
                List.of("src/test/java"), TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
    }
}
