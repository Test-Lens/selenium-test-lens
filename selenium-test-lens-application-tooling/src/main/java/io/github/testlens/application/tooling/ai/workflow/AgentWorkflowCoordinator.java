package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.json.StrictJson;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Request/response coordinator that drives the existing deterministic workflow reducer.
 * Source repair remains a proposal and is never applied here.
 * @since 0.5.0
 */
public final class AgentWorkflowCoordinator {
    private final TestEngineeringWorkflow workflow;
    private final AgentExecutor agents;
    private final TargetedJavaCompiler compiler;
    private final CompiledTargetedTestExecutor tests;
    private final GeneratedTestPolicyValidator validator;
    private final FailureClassifier classifier;
    private final Stabilizer stabilizer;
    private final Configuration configuration;

    public AgentWorkflowCoordinator(TestEngineeringWorkflow workflow, AgentExecutor agents,
                                    TargetedJavaCompiler compiler, TargetedTestExecutor tests,
                                    GeneratedTestPolicyValidator validator, FailureClassifier classifier,
                                    Stabilizer stabilizer, Configuration configuration) {
        this(workflow, agents, compiler, (request, ignoredOutput) -> tests.execute(request), validator,
                classifier, stabilizer, configuration);
    }

    public AgentWorkflowCoordinator(TestEngineeringWorkflow workflow, AgentExecutor agents,
                                    TargetedJavaCompiler compiler, CompiledTargetedTestExecutor tests,
                                    GeneratedTestPolicyValidator validator, FailureClassifier classifier,
                                    Stabilizer stabilizer, Configuration configuration) {
        this.workflow = java.util.Objects.requireNonNull(workflow, "workflow");
        this.agents = java.util.Objects.requireNonNull(agents, "agents");
        this.compiler = java.util.Objects.requireNonNull(compiler, "compiler");
        this.tests = java.util.Objects.requireNonNull(tests, "tests");
        this.validator = java.util.Objects.requireNonNull(validator, "validator");
        this.classifier = java.util.Objects.requireNonNull(classifier, "classifier");
        this.stabilizer = java.util.Objects.requireNonNull(stabilizer, "stabilizer");
        this.configuration = java.util.Objects.requireNonNull(configuration, "configuration");
    }

    public Result run(String runId, TestEngineeringRequest request, AgentContextPack context)
            throws AgentExecutor.AgentExecutionException {
        java.util.Objects.requireNonNull(request, "request");
        java.util.Objects.requireNonNull(context, "context");
        MetricsAccumulator metrics = new MetricsAccumulator();
        List<Step> steps = new ArrayList<>();
        TestEngineeringRun run = TestEngineeringRun.create(runId, request);
        ArtifactEnvelope<AgentContextPack> contextArtifact = ArtifactEnvelope.create("context", runId, List.of(), 1, context);
        run = workflow.reduce(run, new TestEngineeringWorkflow.Event(
                TestEngineeringWorkflow.EventType.PREPARE_CONTEXT, contextArtifact, "bounded context", null));
        steps.add(step(steps, "CONTEXT_PREPARED", Status.PASS, 0, StrictJson.write(context).length, 0,
                context.completeness().name()));

        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
        AgentCall planCall = call(runId, AgentExecutor.Role.TEST_ARCHITECT, List.of(contextArtifact),
                Map.of("expectedResult", "TestPlan"), metrics);
        if (!(planCall.payload() instanceof TestPlan plan)) {
            throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                    "Architect did not return TestPlan");
        }
        requireUsableHeader("Architect", plan.header());
        ArtifactEnvelope<TestPlan> planArtifact = ArtifactEnvelope.create("plan-1", runId,
                List.of(contextArtifact.artifactId()), 1, plan);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.PLAN_PRODUCED, planArtifact));
        steps.add(step(steps, "PLAN_READY", Status.PASS, planCall.durationMillis(), planCall.inputBytes(),
                planCall.outputBytes(), "scenarios=" + plan.scenarios().size()));

        int implementationAttempt = 0;
        ArtifactEnvelope<TestImplementationProposal> implementationArtifact = null;
        TargetedJavaCompiler.CompilationResult compilation = null;
        while (implementationAttempt < configuration.maxImplementationAttempts()) {
            implementationAttempt++;
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(implementationAttempt == 1
                    ? TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION
                    : TestEngineeringWorkflow.EventType.REQUEST_CORRECTION));
            List<ArtifactEnvelope<?>> implementationInputs = new ArrayList<>(List.of(contextArtifact, planArtifact));
            if (implementationArtifact != null) implementationInputs.add(implementationArtifact);
            if (compilation != null) {
                CompilationFeedback feedback = CompilationFeedback.from(compilation);
                implementationInputs.add(ArtifactEnvelope.create("compile-feedback-" + (implementationAttempt - 1), runId,
                        List.of(implementationArtifact.artifactId()), implementationAttempt - 1, feedback));
            }
            AgentCall implementationCall = call(runId, AgentExecutor.Role.TEST_IMPLEMENTER, implementationInputs,
                    Map.of("expectedResult", "TestImplementationProposal",
                            "attempt", Integer.toString(implementationAttempt)), metrics);
            if (implementationCall.payload() instanceof PageObjectCapabilityMissing missing) {
                ArtifactEnvelope<PageObjectCapabilityMissing> missingArtifact = ArtifactEnvelope.create(
                        "capability-missing", runId, List.of(planArtifact.artifactId()), implementationAttempt, missing);
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                        TestEngineeringWorkflow.EventType.CAPABILITY_MISSING, missingArtifact));
                steps.add(step(steps, "CAPABILITY_MISSING", Status.BLOCKED, implementationCall.durationMillis(),
                        implementationCall.inputBytes(), implementationCall.outputBytes(), missing.reason()));
                return result(run, steps, metrics);
            }
            if (implementationCall.payload() instanceof PageObjectExtensionProposal extension) {
                ArtifactEnvelope<PageObjectExtensionProposal> extensionArtifact = ArtifactEnvelope.create(
                        "extension-proposal", runId, List.of(planArtifact.artifactId()), implementationAttempt, extension);
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                        TestEngineeringWorkflow.EventType.CAPABILITY_MISSING, extensionArtifact));
                steps.add(step(steps, "CAPABILITY_MISSING", Status.BLOCKED, implementationCall.durationMillis(),
                        implementationCall.inputBytes(), implementationCall.outputBytes(), extension.proposedMethod()));
                return result(run, steps, metrics);
            }
            if (!(implementationCall.payload() instanceof TestImplementationProposal implementation)) {
                throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                        "Implementer did not return a supported structured result");
            }
            requireUsableHeader("Implementer", implementation.header());
            String implementationId = "implementation-" + implementationAttempt;
            implementationArtifact = ArtifactEnvelope.create(implementationId, runId,
                    List.of(planArtifact.artifactId()), implementationAttempt, implementation);
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                    TestEngineeringWorkflow.EventType.IMPLEMENTATION_PRODUCED, implementationArtifact));
            steps.add(step(steps, "IMPLEMENTATION_READY", Status.PASS, implementationCall.durationMillis(),
                    implementationCall.inputBytes(), implementationCall.outputBytes(),
                    "attempt=" + implementationAttempt));

            GeneratedTestPolicyValidator.ValidationResult policy = validator.validate(request,
                    configuration.sourcePath(), implementation.sourcePatch());
            if (implementation.selectorAccessPolicy() != TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY
                    || !policy.accepted()) {
                run = workflow.reduce(run, new TestEngineeringWorkflow.Event(
                        TestEngineeringWorkflow.EventType.FAIL, null,
                        "generated test policy rejected " + policy.violations().stream()
                                .map(value -> value.rule().name()).toList(), null));
                steps.add(step(steps, "POLICY_VALIDATION", Status.FAIL, 0, 0, 0,
                        policy.violations().stream().map(value -> value.rule().name()).distinct().sorted().toList().toString()));
                return result(run, steps, metrics);
            }
            steps.add(step(steps, "POLICY_VALIDATION", Status.PASS, 0, 0, 0, "Page Objects only"));

            long compileStart = System.nanoTime();
            compilation = compile(implementation);
            long compileMillis = elapsed(compileStart);
            metrics.compileDurationMillis += compileMillis;
            metrics.compileAttempts++;
            if (compilation.successful()) {
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                        TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED));
                steps.add(step(steps, "COMPILE_PASS", Status.PASS, compileMillis, 0, 0,
                        "classes=" + compilation.generatedClasses()));
                break;
            }
            ArtifactEnvelope<TargetedJavaCompiler.CompilationResult> compileArtifact = ArtifactEnvelope.create(
                    "compile-" + implementationAttempt, runId, List.of(implementationArtifact.artifactId()),
                    implementationAttempt, compilation);
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                    TestEngineeringWorkflow.EventType.COMPILE_FAILED, compileArtifact));
            steps.add(step(steps, "COMPILE_FAIL", Status.FAIL, compileMillis, 0, 0,
                    CompilationFeedback.from(compilation).diagnostics().toString()));
            if (implementationAttempt >= configuration.maxImplementationAttempts()) {
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                        TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
                return result(run, steps, metrics);
            }
        }

        if (compilation == null || !compilation.successful() || implementationArtifact == null) {
            throw new IllegalStateException("Compilation loop ended without a successful implementation");
        }
        long executionStart = System.nanoTime();
        TargetedTestExecutor.ExecutionResult targeted;
        try {
            targeted = tests.execute(new TargetedTestExecutor.ExecutionRequest(runId,
                    configuration.binaryName(), configuration.testSelectors(), configuration.executionTimeout()),
                    compilation.output());
        } catch (Exception failure) {
            targeted = new TargetedTestExecutor.ExecutionResult(false, 0,
                    List.of("targeted runner failed: " + failure.getClass().getSimpleName()));
        }
        long executionMillis = elapsed(executionStart);
        metrics.executionDurationMillis += executionMillis;
        metrics.executionAttempts++;
        TestExecutionResult execution = execution(targeted, executionMillis, request.target().scenarioId());
        ArtifactEnvelope<TestExecutionResult> executionArtifact = ArtifactEnvelope.create("execution-1", runId,
                List.of(implementationArtifact.artifactId()), 1, execution);
        if (targeted.successful()) {
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                    TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED, executionArtifact));
            steps.add(step(steps, "EXECUTION_PASS", Status.PASS, executionMillis, 0, 0,
                    "tests=" + targeted.tests()));
            AgentCall reviewCall = call(runId, AgentExecutor.Role.CODE_REVIEWER,
                    List.of(contextArtifact, planArtifact, implementationArtifact, executionArtifact),
                    Map.of("expectedResult", "CodeReviewResult"), metrics);
            if (!(reviewCall.payload() instanceof CodeReviewResult review)) {
                throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                        "Reviewer did not return CodeReviewResult");
            }
            requireUsableHeader("Reviewer", review.header());
            ArtifactEnvelope<CodeReviewResult> reviewArtifact = ArtifactEnvelope.create("review-1", runId,
                    List.of(executionArtifact.artifactId()), 1, review);
            TestEngineeringWorkflow.EventType reviewEvent = review.verdict() == CodeReviewResult.Verdict.APPROVE
                    ? TestEngineeringWorkflow.EventType.REVIEW_APPROVED
                    : TestEngineeringWorkflow.EventType.REVIEW_REJECTED;
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(reviewEvent, reviewArtifact));
            steps.add(step(steps, "REVIEW", review.verdict() == CodeReviewResult.Verdict.APPROVE
                    ? Status.APPROVE : Status.REQUEST_CHANGES, reviewCall.durationMillis(), reviewCall.inputBytes(),
                    reviewCall.outputBytes(), review.verdict().name()));
            return result(run, steps, metrics);
        }

        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.EXECUTION_FAILED, executionArtifact));
        steps.add(step(steps, "EXECUTION_FAIL", Status.FAIL, executionMillis, 0, 0,
                execution.failureSummary()));
        FailureClassification classification = classifier.classify(execution);
        ArtifactEnvelope<FailureClassification> classificationArtifact = ArtifactEnvelope.create("classification-1", runId,
                List.of(executionArtifact.artifactId()), 1, classification);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.CLASSIFY_FAILURE, classificationArtifact));
        steps.add(step(steps, "FAILURE_CLASSIFIED", Status.PASS, 0, 0, 0, classification.category().name()));
        Optional<RepairProposal> repair = stabilizer.propose(classification, execution);
        if (repair.isPresent()) {
            ArtifactEnvelope<RepairProposal> repairArtifact = ArtifactEnvelope.create("repair-1", runId,
                    List.of(classificationArtifact.artifactId()), 1, repair.orElseThrow());
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                    TestEngineeringWorkflow.EventType.PROPOSE_REPAIR, repairArtifact));
            steps.add(step(steps, "REPAIR_PROPOSED", Status.PROPOSE_ONLY, 0, 0, 0,
                    repair.orElseThrow().proposalId()));
            return result(run, steps, metrics);
        }
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
        return result(run, steps, metrics);
    }

    private AgentCall call(String runId, AgentExecutor.Role role, List<ArtifactEnvelope<?>> inputs,
                           Map<String, String> instructions, MetricsAccumulator metrics)
            throws AgentExecutor.AgentExecutionException {
        AgentExecutor.AgentCommand command = new AgentExecutor.AgentCommand(runId, role, inputs, instructions);
        long inputBytes = StrictJson.write(command).length;
        long start = System.nanoTime();
        AgentExecutor.AgentResult result = agents.execute(command);
        long duration = elapsed(start);
        long outputBytes = StrictJson.write(result.artifact().payload()).length;
        metrics.agentInputBytes += inputBytes;
        metrics.agentOutputBytes += outputBytes;
        metrics.agentDurationMillis += duration;
        metrics.agentAttempts++;
        return new AgentCall(result.artifact().payload(), inputBytes, outputBytes, duration);
    }

    private TargetedJavaCompiler.CompilationResult compile(TestImplementationProposal implementation) {
        String current = configuration.currentSource();
        String expected = ArtifactEnvelope.digest(current == null ? new byte[0]
                : current.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var unit = new TargetedJavaCompiler.SourceUnit(configuration.sourcePath(), configuration.binaryName(),
                implementation.sourcePatch(), expected);
        var request = new TargetedJavaCompiler.CompilationRequest(List.of(unit), configuration.release(),
                configuration.classpath(), configuration.maxDiagnostics());
        return compiler.compile(request, current == null ? Map.of() : Map.of(configuration.sourcePath(), current));
    }

    private static TestExecutionResult execution(TargetedTestExecutor.ExecutionResult result, long duration,
                                                 String scenarioId) {
        TestExecutionResult.Outcome outcome = switch (result.status()) {
            case PASS -> TestExecutionResult.Outcome.PASS;
            case FAIL -> TestExecutionResult.Outcome.FAIL;
            case TIMED_OUT -> TestExecutionResult.Outcome.TIMED_OUT;
        };
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION,
                result.successful() ? ContractHeader.Status.COMPLETED : ContractHeader.Status.FAILED,
                result.boundedEvidence(), List.of(), ContractHeader.Confidence.OBSERVED);
        return new TestExecutionResult(header, scenarioId, TestExecutionResult.Outcome.PASS, outcome,
                "tests=" + result.tests(), duration, List.of("compile:pass"), result.boundedEvidence(), List.of(),
                result.boundedEvidence(), List.of(), List.of(), result.successful() ? null : "Targeted test failed");
    }

    private static Step step(List<Step> existing, String name, Status status, long duration,
                             long inputBytes, long outputBytes, String summary) {
        return new Step(existing.size() + 1, name, status, duration, inputBytes, outputBytes,
                summary == null ? "" : summary);
    }

    private static long elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private static void requireUsableHeader(String role, ContractHeader header)
            throws AgentExecutor.AgentExecutionException {
        if (header == null || switch (header.status()) {
            case READY, PARTIAL, COMPLETED -> false;
            case BLOCKED, FAILED, UNKNOWN -> true;
        }) {
            throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                    role + " returned a non-actionable contract status: "
                            + (header == null ? "missing" : header.status()));
        }
    }

    private static Result result(TestEngineeringRun run, List<Step> steps, MetricsAccumulator metrics) {
        return new Result(run, steps, new Metrics(metrics.agentInputBytes, metrics.agentOutputBytes,
                metrics.agentDurationMillis, metrics.compileDurationMillis, metrics.executionDurationMillis,
                metrics.agentAttempts, metrics.compileAttempts, metrics.executionAttempts));
    }

    @FunctionalInterface
    public interface FailureClassifier { FailureClassification classify(TestExecutionResult execution); }

    @FunctionalInterface
    public interface Stabilizer {
        Optional<RepairProposal> propose(FailureClassification classification, TestExecutionResult execution);
        static Stabilizer none() { return (classification, execution) -> Optional.empty(); }
    }

    public record Configuration(Path sourcePath, String binaryName, String currentSource, int release,
                                String classpath, int maxDiagnostics, Duration executionTimeout,
                                List<String> testSelectors, int maxImplementationAttempts) {
        public Configuration {
            if (sourcePath == null || sourcePath.isAbsolute() || sourcePath.normalize().startsWith("..")
                    || binaryName == null || binaryName.isBlank() || release < 8
                    || release > Runtime.version().feature() || maxDiagnostics < 1 || maxDiagnostics > 1_000
                    || executionTimeout == null || executionTimeout.isZero() || executionTimeout.isNegative()
                    || maxImplementationAttempts < 1 || maxImplementationAttempts > 20) {
                throw new IllegalArgumentException("valid bounded workflow configuration is required");
            }
            sourcePath = sourcePath.normalize();
            classpath = classpath == null ? "" : classpath;
            testSelectors = List.copyOf(testSelectors == null ? List.of() : testSelectors);
        }
    }

    public record Result(TestEngineeringRun run, List<Step> steps, Metrics metrics) {
        public Result { steps = List.copyOf(steps); }
        public WorkflowReport toWorkflowReport(long contextBytes, List<String> limitations) {
            long deterministic = metrics.compileDurationMillis() + metrics.executionDurationMillis();
            long total = deterministic + metrics.agentDurationMillis();
            List<WorkflowReport.Step> reportSteps = steps.stream().map(value -> new WorkflowReport.Step(
                    value.sequence(), value.name(), WorkflowReport.Status.valueOf(value.status().name()),
                    value.durationMillis(), value.inputBytes(), value.outputBytes(), value.summary(), List.of())).toList();
            WorkflowReport.Metrics reportMetrics = new WorkflowReport.Metrics(contextBytes,
                    metrics.agentInputBytes(), metrics.agentOutputBytes(), metrics.agentDurationMillis(), deterministic,
                    metrics.compileDurationMillis(), metrics.executionDurationMillis(), total,
                    metrics.agentAttempts(), metrics.compileAttempts(), metrics.executionAttempts(), 0,
                    (int) steps.stream().filter(value -> value.status() == Status.PROPOSE_ONLY).count());
            return WorkflowReport.from(run, reportSteps, reportMetrics, limitations);
        }
    }
    public record Step(int sequence, String name, Status status, long durationMillis,
                       long inputBytes, long outputBytes, String summary) { }
    public record Metrics(long agentInputBytes, long agentOutputBytes, long agentDurationMillis,
                          long compileDurationMillis, long executionDurationMillis,
                          int agentAttempts, int compileAttempts, int executionAttempts) { }
    public enum Status { PASS, FAIL, BLOCKED, PROPOSE_ONLY, APPROVE, REQUEST_CHANGES }

    private record AgentCall(Object payload, long inputBytes, long outputBytes, long durationMillis) { }
    public record CompilationFeedback(List<Diagnostic> diagnostics) {
        public CompilationFeedback { diagnostics = List.copyOf(diagnostics); }
        static CompilationFeedback from(TargetedJavaCompiler.CompilationResult result) {
            return new CompilationFeedback(result.diagnostics().stream().map(value -> new Diagnostic(value.code(),
                    value.path() == null ? null : value.path().toString().replace('\\', '/'), value.line(),
                    value.column(), value.message())).toList());
        }
        public record Diagnostic(String code, String path, long line, long column, String message) { }
    }

    private static final class MetricsAccumulator {
        private long agentInputBytes;
        private long agentOutputBytes;
        private long agentDurationMillis;
        private long compileDurationMillis;
        private long executionDurationMillis;
        private int agentAttempts;
        private int compileAttempts;
        private int executionAttempts;
    }
}
