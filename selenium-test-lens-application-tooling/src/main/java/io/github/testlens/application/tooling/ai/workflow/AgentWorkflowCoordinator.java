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

import java.nio.charset.StandardCharsets;
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
    private final java.util.function.Supplier<String> currentPageObjectSourceFingerprint;

    public AgentWorkflowCoordinator(TestEngineeringWorkflow workflow, AgentExecutor agents,
                                    TargetedJavaCompiler compiler, TargetedTestExecutor tests,
                                    GeneratedTestPolicyValidator validator, FailureClassifier classifier,
                                    Stabilizer stabilizer, Configuration configuration) {
        this(workflow, agents, compiler, (request, ignoredOutput) -> tests.execute(request), validator,
                classifier, stabilizer, configuration, () -> null);
    }

    public AgentWorkflowCoordinator(TestEngineeringWorkflow workflow, AgentExecutor agents,
                                    TargetedJavaCompiler compiler, TargetedTestExecutor tests,
                                    GeneratedTestPolicyValidator validator, FailureClassifier classifier,
                                    Stabilizer stabilizer, Configuration configuration,
                                    java.util.function.Supplier<String> currentPageObjectSourceFingerprint) {
        this(workflow, agents, compiler, (request, ignoredOutput) -> tests.execute(request), validator,
                classifier, stabilizer, configuration, currentPageObjectSourceFingerprint);
    }

    public AgentWorkflowCoordinator(TestEngineeringWorkflow workflow, AgentExecutor agents,
                                    TargetedJavaCompiler compiler, CompiledTargetedTestExecutor tests,
                                    GeneratedTestPolicyValidator validator, FailureClassifier classifier,
                                    Stabilizer stabilizer, Configuration configuration) {
        this(workflow, agents, compiler, tests, validator, classifier, stabilizer, configuration, () -> null);
    }

    public AgentWorkflowCoordinator(TestEngineeringWorkflow workflow, AgentExecutor agents,
                                    TargetedJavaCompiler compiler, CompiledTargetedTestExecutor tests,
                                    GeneratedTestPolicyValidator validator, FailureClassifier classifier,
                                    Stabilizer stabilizer, Configuration configuration,
                                    java.util.function.Supplier<String> currentPageObjectSourceFingerprint) {
        this.workflow = java.util.Objects.requireNonNull(workflow, "workflow");
        this.agents = java.util.Objects.requireNonNull(agents, "agents");
        this.compiler = java.util.Objects.requireNonNull(compiler, "compiler");
        this.tests = java.util.Objects.requireNonNull(tests, "tests");
        this.validator = java.util.Objects.requireNonNull(validator, "validator");
        this.classifier = java.util.Objects.requireNonNull(classifier, "classifier");
        this.stabilizer = java.util.Objects.requireNonNull(stabilizer, "stabilizer");
        this.configuration = java.util.Objects.requireNonNull(configuration, "configuration");
        this.currentPageObjectSourceFingerprint = java.util.Objects.requireNonNull(
                currentPageObjectSourceFingerprint, "currentPageObjectSourceFingerprint");
    }

    public Result run(String runId, TestEngineeringRequest request, AgentContextPack context)
            throws AgentExecutor.AgentExecutionException {
        java.util.Objects.requireNonNull(request, "request");
        java.util.Objects.requireNonNull(context, "context");
        String runSourceFingerprint = currentPageObjectSourceFingerprint(context);
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
        ArtifactEnvelope<CodeReviewResult> previousReviewArtifact = null;
        ArtifactEnvelope<ReviewerEvidence.QualityEvidence> previousQualityArtifact = null;
        ArtifactEnvelope<TestExecutionResult> previousExecutionArtifact = null;
        ArtifactEnvelope<ReviewerEvidence.QualityEvidence> qualityArtifact = null;
        ArtifactEnvelope<ReviewerEvidence.CompilationEvidence> compileSuccessArtifact = null;
        while (implementationAttempt < configuration.maxImplementationAttempts()) {
            implementationAttempt++;
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(implementationAttempt == 1
                    ? TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION
                    : TestEngineeringWorkflow.EventType.REQUEST_CORRECTION));
            List<ArtifactEnvelope<?>> implementationInputs = new ArrayList<>(List.of(contextArtifact, planArtifact));
            if (implementationArtifact != null) implementationInputs.add(implementationArtifact);
            if (previousReviewArtifact != null) {
                implementationInputs.add(previousReviewArtifact);
                implementationInputs.add(previousQualityArtifact);
                implementationInputs.add(previousExecutionArtifact);
            } else if (compilation != null) {
                CompilationFeedback feedback = CompilationFeedback.from(compilation);
                implementationInputs.add(ArtifactEnvelope.create("compile-feedback-" + (implementationAttempt - 1), runId,
                        List.of(implementationArtifact.artifactId()), implementationAttempt - 1, feedback));
            }
            AgentCall implementationCall = call(runId, AgentExecutor.Role.TEST_IMPLEMENTER, implementationInputs,
                    Map.of("expectedResult", "TEST_IMPLEMENTATION_PROPOSAL | PAGE_OBJECT_CAPABILITY_MISSING | PAGE_OBJECT_EXTENSION_PROPOSAL",
                            "attempt", Integer.toString(implementationAttempt),
                            "correctionReason", previousReviewArtifact == null ? "NONE" : "TYPED_REVIEW_FINDINGS",
                            "testLens.pageObjectSourceFingerprint",
                            runSourceFingerprint), metrics);
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
            List<String> implementationParents = previousReviewArtifact == null
                    ? List.of(planArtifact.artifactId())
                    : List.of(planArtifact.artifactId(), implementationArtifact.artifactId(),
                            previousReviewArtifact.artifactId());
            implementationArtifact = ArtifactEnvelope.create(implementationId, runId,
                    implementationParents, implementationAttempt, implementation);
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

            ReviewerEvidence.QualityEvidence quality = ReviewerEvidence.analyze(implementation,
                    runSourceFingerprint);
            qualityArtifact = ArtifactEnvelope.create(
                    "quality-" + implementationAttempt, runId, List.of(implementationArtifact.artifactId()),
                    implementationAttempt, quality);
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                    TestEngineeringWorkflow.EventType.QUALITY_VERIFIED, qualityArtifact));
            steps.add(step(steps, "QUALITY_EVIDENCE",
                    quality.status() == ReviewerEvidence.QualityStatus.COMPLETE ? Status.PASS : Status.BLOCKED,
                    0, 0, 0, "status=" + quality.status() + ",tests=" + quality.junitTestMethodCount()
                            + ",assertions=" + quality.assertionCount()));
            if (!runSourceFingerprint.equals(currentPageObjectSourceFingerprint(context))) {
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                        TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
                steps.add(step(steps, "SOURCE_FINGERPRINT", Status.BLOCKED, 0, 0, 0,
                        ReviewValidationFailure.IMPLEMENTATION_FINGERPRINT_MISMATCH.name()));
                return result(run, steps, metrics);
            }
            long compileStart = System.nanoTime();
            compilation = compile(implementation);
            long compileMillis = elapsed(compileStart);
            metrics.compileDurationMillis += compileMillis;
            metrics.compileAttempts++;
            if (compilation.successful()) {
                ReviewerEvidence.CompilationEvidence compileEvidence = new ReviewerEvidence.CompilationEvidence(
                        ReviewerEvidence.CompilationEvidence.Status.PASS, compilation.generatedClasses(),
                        quality.implementationDigest());
                compileSuccessArtifact = ArtifactEnvelope.create(
                        "compile-success-" + implementationAttempt, runId,
                        List.of(implementationArtifact.artifactId(), qualityArtifact.artifactId()),
                        implementationAttempt, compileEvidence);
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                        TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED, compileSuccessArtifact));
                steps.add(step(steps, "COMPILE_PASS", Status.PASS, compileMillis, 0, 0,
                        "classes=" + compilation.generatedClasses()));
            } else {
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
                continue;
            }

        if (!runSourceFingerprint.equals(currentPageObjectSourceFingerprint(context))) {
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                    TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
            steps.add(step(steps, "SOURCE_FINGERPRINT", Status.BLOCKED, 0, 0, 0,
                    ReviewValidationFailure.IMPLEMENTATION_FINGERPRINT_MISMATCH.name()));
            return result(run, steps, metrics);
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
        TestExecutionResult execution = execution(targeted, executionMillis, request.target().scenarioId(),
                compileSuccessArtifact.artifactId());
        ArtifactEnvelope<TestExecutionResult> executionArtifact = ArtifactEnvelope.create(
                "execution-" + implementationAttempt, runId,
                List.of(implementationArtifact.artifactId(), compileSuccessArtifact.artifactId()),
                implementationAttempt, execution);
        if (targeted.successful()) {
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                    TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED, executionArtifact));
            steps.add(step(steps, "EXECUTION_PASS", Status.PASS, executionMillis, 0, 0,
                    "tests=" + targeted.tests()));
            List<ArtifactEnvelope<?>> evidenceArtifacts = List.of(contextArtifact, planArtifact, implementationArtifact,
                    compileSuccessArtifact, qualityArtifact, executionArtifact);
            List<String> allowedReviewRefs = evidenceArtifacts.stream().map(ArtifactEnvelope::artifactId).toList();
            List<String> requiredReviewRefs = List.of(implementationArtifact.artifactId(),
                    compileSuccessArtifact.artifactId(), qualityArtifact.artifactId(), executionArtifact.artifactId());
            ReviewerEvidencePack evidencePack = evidencePack(runId, implementationAttempt, evidenceArtifacts,
                    requiredReviewRefs, implementationArtifact, currentPageObjectSourceFingerprint(context));
            ArtifactEnvelope<ReviewerEvidencePack> evidencePackArtifact = ArtifactEnvelope.create(
                    "review-evidence-" + implementationAttempt, runId, requiredReviewRefs,
                    implementationAttempt, evidencePack);
            List<ArtifactEnvelope<?>> reviewInputs = new ArrayList<>(evidenceArtifacts);
            reviewInputs.add(evidencePackArtifact);
            AgentCall reviewCall = call(runId, AgentExecutor.Role.CODE_REVIEWER, reviewInputs,
                    Map.of("expectedResult", "CodeReviewResult",
                            "reviewerEvidenceCatalog", evidencePackArtifact.artifactId(),
                            "allowedReviewArtifactRefs", String.join(",", allowedReviewRefs),
                            "requiredReviewedArtifactRefs", String.join(",", requiredReviewRefs),
                            "allowedReviewRuleIds", String.join(",", evidencePack.allowedRuleIds())), metrics);
            if (!(reviewCall.payload() instanceof CodeReviewResult review)) {
                throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                        "Reviewer did not return CodeReviewResult");
            }
            requireUsableHeader("Reviewer", review.header());
            ReviewValidation reviewValidation = validateReview(review, allowedReviewRefs, requiredReviewRefs,
                    qualityArtifact.payload(), execution, implementationArtifact, compileSuccessArtifact,
                    currentPageObjectSourceFingerprint(context), evidencePack, evidenceArtifacts);
            if (!reviewValidation.accepted()) {
                CodeReviewResult blocked = hostBlockedReview(reviewValidation.reason(), requiredReviewRefs);
                ArtifactEnvelope<CodeReviewResult> blockedArtifact = ArtifactEnvelope.create(
                        "review-block-" + implementationAttempt, runId,
                        requiredReviewRefs, implementationAttempt, blocked);
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                        TestEngineeringWorkflow.EventType.REVIEW_REJECTED, blockedArtifact));
                steps.add(step(steps, "REVIEW_CONTRACT", Status.BLOCKED, reviewCall.durationMillis(),
                        reviewCall.inputBytes(), reviewCall.outputBytes(), reviewValidation.reason().name()));
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                        TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
                return result(run, steps, metrics);
            }
            ArtifactEnvelope<CodeReviewResult> reviewArtifact = ArtifactEnvelope.create(
                    "review-" + implementationAttempt, runId,
                    List.of(implementationArtifact.artifactId(), compileSuccessArtifact.artifactId(),
                            qualityArtifact.artifactId(), executionArtifact.artifactId()),
                    implementationAttempt, review);
            TestEngineeringWorkflow.EventType reviewEvent = review.verdict() == CodeReviewResult.Verdict.APPROVE
                    ? TestEngineeringWorkflow.EventType.REVIEW_APPROVED
                    : TestEngineeringWorkflow.EventType.REVIEW_REJECTED;
            run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(reviewEvent, reviewArtifact));
            Status reviewStatus = switch (review.verdict()) {
                case APPROVE -> Status.APPROVE;
                case REQUEST_CHANGES -> Status.REQUEST_CHANGES;
                case BLOCK -> Status.BLOCKED;
            };
            steps.add(step(steps, "REVIEW", reviewStatus, reviewCall.durationMillis(), reviewCall.inputBytes(),
                    reviewCall.outputBytes(), review.verdict().name()));
            if (review.verdict() == CodeReviewResult.Verdict.APPROVE) return result(run, steps, metrics);
            if (review.verdict() == CodeReviewResult.Verdict.BLOCK) {
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                        TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
                return result(run, steps, metrics);
            }
            if (!workflow.canRequestCorrection(run)
                    || implementationAttempt >= configuration.maxImplementationAttempts()) {
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(
                        TestEngineeringWorkflow.EventType.REQUIRE_HUMAN_REVIEW));
                return result(run, steps, metrics);
            }
            previousReviewArtifact = reviewArtifact;
            previousQualityArtifact = qualityArtifact;
            previousExecutionArtifact = executionArtifact;
            continue;
        }

        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.EXECUTION_FAILED, executionArtifact));
        steps.add(step(steps, "EXECUTION_FAIL", Status.FAIL, executionMillis, 0, 0,
                execution.failureSummary()));
        FailureClassification classification = classifier.classify(execution);
        ArtifactEnvelope<FailureClassification> classificationArtifact = ArtifactEnvelope.create(
                "classification-" + implementationAttempt, runId,
                List.of(executionArtifact.artifactId()), implementationAttempt, classification);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.CLASSIFY_FAILURE, classificationArtifact));
        steps.add(step(steps, "FAILURE_CLASSIFIED", Status.PASS, 0, 0, 0, classification.category().name()));
        Optional<RepairProposal> repair = stabilizer.propose(classification, execution);
        if (repair.isPresent()) {
            ArtifactEnvelope<RepairProposal> repairArtifact = ArtifactEnvelope.create(
                    "repair-" + implementationAttempt, runId,
                    List.of(classificationArtifact.artifactId()), implementationAttempt, repair.orElseThrow());
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
        throw new IllegalStateException("Implementation attempt bound ended without a terminal result");
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

    private String currentPageObjectSourceFingerprint(AgentContextPack context) {
        String value = currentPageObjectSourceFingerprint.get();
        if (value != null && !value.isBlank()) return value;
        if (context.pageObjectCapabilities() == null
                || context.pageObjectCapabilities().sourceFingerprint() == null) return "";
        return context.pageObjectCapabilities().sourceFingerprint();
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
                                                 String scenarioId, String compileEvidenceRef) {
        TestExecutionResult.Outcome outcome = switch (result.status()) {
            case PASS -> TestExecutionResult.Outcome.PASS;
            case FAIL -> TestExecutionResult.Outcome.FAIL;
            case TIMED_OUT -> TestExecutionResult.Outcome.TIMED_OUT;
        };
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION,
                result.successful() ? ContractHeader.Status.COMPLETED : ContractHeader.Status.FAILED,
                result.boundedEvidence(), List.of(), ContractHeader.Confidence.OBSERVED);
        return new TestExecutionResult(header, scenarioId, TestExecutionResult.Outcome.PASS, outcome,
                "tests=" + result.tests(), duration, List.of(compileEvidenceRef), result.boundedEvidence(), List.of(),
                result.boundedEvidence(), List.of(), List.of(), result.successful() ? null : "Targeted test failed");
    }

    private static ReviewValidation validateReview(CodeReviewResult review, List<String> allowedRefs,
                                                   List<String> requiredRefs,
                                                   ReviewerEvidence.QualityEvidence quality,
                                                   TestExecutionResult execution,
                                                   ArtifactEnvelope<TestImplementationProposal> implementationArtifact,
                                                   ArtifactEnvelope<ReviewerEvidence.CompilationEvidence> compilationArtifact,
                                                   String currentSourceFingerprint,
                                                   ReviewerEvidencePack evidencePack,
                                                   List<ArtifactEnvelope<?>> evidenceArtifacts) {
        ReviewerEvidencePack.IntegrityStatus integrity=evidencePack.verify(evidenceArtifacts,currentSourceFingerprint);
        if(integrity!=ReviewerEvidencePack.IntegrityStatus.VALID){
            return new ReviewValidation(false,switch(integrity){
                case EVIDENCE_UNAVAILABLE->ReviewValidationFailure.EVIDENCE_UNAVAILABLE;
                case EVIDENCE_DIGEST_MISMATCH->ReviewValidationFailure.EVIDENCE_DIGEST_MISMATCH;
                case EVIDENCE_LINEAGE_INVALID->ReviewValidationFailure.EVIDENCE_LINEAGE_INVALID;
                case IMPLEMENTATION_FINGERPRINT_MISMATCH->ReviewValidationFailure.IMPLEMENTATION_FINGERPRINT_MISMATCH;
                case VALID->throw new IllegalStateException("valid evidence cannot be rejected");
            });
        }
        String currentImplementationDigest = ArtifactEnvelope.digest(
                implementationArtifact.payload().sourcePatch().getBytes(StandardCharsets.UTF_8));
        if (!quality.implementationDigest().equals(currentImplementationDigest)
                || !compilationArtifact.payload().implementationDigest().equals(currentImplementationDigest)
                || !compilationArtifact.parents().contains(implementationArtifact.artifactId())
                || currentSourceFingerprint == null || currentSourceFingerprint.isBlank()
                || !quality.sourceFingerprint().equals(currentSourceFingerprint)) {
            return new ReviewValidation(false, currentSourceFingerprint == null
                    || !quality.sourceFingerprint().equals(currentSourceFingerprint)
                    ? ReviewValidationFailure.IMPLEMENTATION_FINGERPRINT_MISMATCH
                    : ReviewValidationFailure.EVIDENCE_DIGEST_MISMATCH);
        }
        if (!allowedRefs.containsAll(review.reviewedArtifactRefs())
                || !review.reviewedArtifactRefs().containsAll(requiredRefs)) {
            return new ReviewValidation(false, ReviewValidationFailure.ARTIFACT_REFERENCE_INVALID);
        }
        if (review.findings().stream().anyMatch(finding -> !allowedRefs.contains(finding.artifactRef()))) {
            return new ReviewValidation(false, ReviewValidationFailure.FINDING_ARTIFACT_INVALID);
        }
        if (review.findings().stream().anyMatch(finding -> !findingAllowed(evidencePack, finding))) {
            return new ReviewValidation(false, ReviewValidationFailure.FEEDBACK_UNSUBSTANTIATED);
        }
        if (review.verdict() == CodeReviewResult.Verdict.APPROVE) {
            if (!review.findings().isEmpty() || !quality.completeForApproval()
                    || execution.compileOutcome() != TestExecutionResult.Outcome.PASS
                    || execution.executionOutcome() != TestExecutionResult.Outcome.PASS
                    || !hasExecutedTests(execution.frameworkResult())
                    || execution.compileEvidenceRefs().isEmpty()) {
                return new ReviewValidation(false, ReviewValidationFailure.APPROVAL_EVIDENCE_INCOMPLETE);
            }
            return new ReviewValidation(true, ReviewValidationFailure.NONE);
        }
        boolean substantiated = !review.findings().isEmpty() && review.findings().stream().allMatch(finding ->
                finding.severity() == CodeReviewResult.Severity.ERROR
                        && finding.evidenceAvailability() == CodeReviewResult.EvidenceAvailability.VERIFIED
                        && findingSupportedByQuality(finding, quality));
        return substantiated
                ? new ReviewValidation(true, ReviewValidationFailure.NONE)
                : new ReviewValidation(false, ReviewValidationFailure.FEEDBACK_UNSUBSTANTIATED);
    }

    private static boolean findingAllowed(ReviewerEvidencePack pack, CodeReviewResult.Finding finding) {
        return pack.evidence().stream().filter(value -> value.artifactId().equals(finding.artifactRef()))
                .anyMatch(value -> value.availability() == ReviewerEvidencePack.Availability.AVAILABLE
                        && value.supportedFindingContracts().stream().anyMatch(contract -> contract.matches(finding)));
    }

    private static ReviewerEvidencePack evidencePack(String runId, int attempt,
                                                     List<ArtifactEnvelope<?>> artifacts,
                                                     List<String> requiredRefs,
                                                     ArtifactEnvelope<TestImplementationProposal> implementation,
                                                     String sourceFingerprint) {
        List<ReviewerEvidencePack.Evidence> catalog = artifacts.stream().map(artifact -> evidenceEntry(
                artifact, requiredRefs.contains(artifact.artifactId()))).toList();
        String implementationDigest = ArtifactEnvelope.digest(implementation.payload().sourcePatch()
                .getBytes(StandardCharsets.UTF_8));
        List<String> allowedRules = catalog.stream().flatMap(value -> value.allowedRuleIds().stream())
                .distinct().sorted().toList();
        return new ReviewerEvidencePack(runId, attempt, implementation.artifactId(), implementationDigest,
                sourceFingerprint, catalog, requiredRefs, allowedRules);
    }

    private static ReviewerEvidencePack.Evidence evidenceEntry(ArtifactEnvelope<?> artifact,
                                                               boolean requiredForApproval) {
        ReviewerEvidencePack.ArtifactType type;
        List<CodeReviewResult.Category> categories;
        List<String> rules;
        Object payload = artifact.payload();
        if (payload instanceof AgentContextPack) {
            type = ReviewerEvidencePack.ArtifactType.CONTEXT;
            categories = List.of(CodeReviewResult.Category.REQUIREMENT_NOT_COVERED);
            rules = List.of(CodeReviewResult.RuleId.REQUIREMENT_COVERAGE_REQUIRED.name());
        } else if (payload instanceof TestPlan) {
            type = ReviewerEvidencePack.ArtifactType.PLAN;
            categories = List.of(CodeReviewResult.Category.REQUIREMENT_NOT_COVERED);
            rules = List.of(CodeReviewResult.RuleId.REQUIREMENT_COVERAGE_REQUIRED.name());
        } else if (payload instanceof TestImplementationProposal) {
            type = ReviewerEvidencePack.ArtifactType.IMPLEMENTATION;
            categories = List.of();
            rules = List.of();
        } else if (payload instanceof ReviewerEvidence.CompilationEvidence) {
            type = ReviewerEvidencePack.ArtifactType.COMPILATION;
            categories = List.of();
            rules = List.of();
        } else if (payload instanceof ReviewerEvidence.QualityEvidence) {
            type = ReviewerEvidencePack.ArtifactType.QUALITY;
            categories = List.of(CodeReviewResult.Category.ASSERTION_INSUFFICIENT,
                    CodeReviewResult.Category.REQUIREMENT_NOT_COVERED,
                    CodeReviewResult.Category.TEST_LIFECYCLE_PROBLEM);
            rules = List.of(CodeReviewResult.RuleId.ASSERTION_REQUIRED.name(),
                    CodeReviewResult.RuleId.REQUIREMENT_COVERAGE_REQUIRED.name(),
                    CodeReviewResult.RuleId.LIFECYCLE_INTEGRITY.name());
        } else if (payload instanceof TestExecutionResult) {
            type = ReviewerEvidencePack.ArtifactType.EXECUTION;
            categories = List.of();
            rules = List.of();
        } else {
            throw new IllegalArgumentException("unsupported reviewer evidence payload");
        }
        return new ReviewerEvidencePack.Evidence(artifact.artifactId(), type, artifact.runId(), artifact.attempt(),
                artifact.payloadDigest(), artifact.parents(), ReviewerEvidencePack.Availability.AVAILABLE,
                requiredForApproval, categories, rules);
    }

    private static CodeReviewResult hostBlockedReview(ReviewValidationFailure failure, List<String> reviewedRefs) {
        CodeReviewResult.RuleId rule = switch (failure) {
            case ARTIFACT_REFERENCE_INVALID, FINDING_ARTIFACT_INVALID -> CodeReviewResult.RuleId.ARTIFACT_REFERENCE_INVALID;
            case EVIDENCE_UNAVAILABLE -> CodeReviewResult.RuleId.EVIDENCE_UNAVAILABLE;
            case EVIDENCE_DIGEST_MISMATCH -> CodeReviewResult.RuleId.EVIDENCE_DIGEST_MISMATCH;
            case EVIDENCE_LINEAGE_INVALID -> CodeReviewResult.RuleId.EVIDENCE_LINEAGE_INVALID;
            case IMPLEMENTATION_FINGERPRINT_MISMATCH -> CodeReviewResult.RuleId.IMPLEMENTATION_FINGERPRINT_MISMATCH;
            case FEEDBACK_UNSUBSTANTIATED -> CodeReviewResult.RuleId.FEEDBACK_UNSUBSTANTIATED;
            case APPROVAL_EVIDENCE_INCOMPLETE, DECISION_CONTRADICTORY -> CodeReviewResult.RuleId.DECISION_CONTRADICTORY;
            case NONE -> throw new IllegalArgumentException("cannot block an accepted review");
        };
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.BLOCKED,
                List.of(), List.of(), ContractHeader.Confidence.OBSERVED);
        CodeReviewResult.EvidenceAvailability availability = switch (failure) {
            case EVIDENCE_UNAVAILABLE -> CodeReviewResult.EvidenceAvailability.UNAVAILABLE;
            case EVIDENCE_DIGEST_MISMATCH, EVIDENCE_LINEAGE_INVALID,
                    IMPLEMENTATION_FINGERPRINT_MISMATCH -> CodeReviewResult.EvidenceAvailability.PARTIAL;
            default -> CodeReviewResult.EvidenceAvailability.VERIFIED;
        };
        CodeReviewResult.Finding finding = new CodeReviewResult.Finding(
                CodeReviewResult.Category.REVIEW_UNSUBSTANTIATED, CodeReviewResult.Severity.ERROR, rule.name(),
                reviewedRefs.get(0), CodeReviewResult.ProblemCategory.REVIEW_CONTRACT,
                CodeReviewResult.RequiredAction.HUMAN_REVIEW, CodeReviewResult.SafeLocation.EXECUTION_EVIDENCE,
                availability);
        return new CodeReviewResult(header, CodeReviewResult.Verdict.BLOCK, reviewedRefs, List.of(finding));
    }

    private static boolean findingSupportedByQuality(CodeReviewResult.Finding finding,
                                                     ReviewerEvidence.QualityEvidence quality) {
        return switch (finding.category()) {
            case ASSERTION_INSUFFICIENT -> quality.assertionCount() == 0
                    || quality.requiredApiInsideAssertion() == ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED;
            case TEST_LIFECYCLE_PROBLEM -> quality.controlPath() == ReviewerEvidence.ControlPathStatus.EARLY_RETURN_PRESENT
                    || quality.swallowedFailures() == ReviewerEvidence.SwallowedFailureStatus.PRESENT;
            case EXECUTION_EVIDENCE_INSUFFICIENT -> false;
            case REQUIREMENT_NOT_COVERED -> quality.requiredApiInsideAssertion()
                    == ReviewerEvidence.RequiredApiAssertionStatus.NOT_VERIFIED;
            case PAGE_OBJECT_MISUSE, TEST_DATA_INVALID, UNSAFE_IMPLEMENTATION, REVIEW_UNSUBSTANTIATED -> false;
        };
    }

    private static boolean hasExecutedTests(String frameworkResult) {
        if (frameworkResult == null || !frameworkResult.startsWith("tests=")) return false;
        try {
            return Integer.parseInt(frameworkResult.substring("tests=".length())) > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
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
    private record ReviewValidation(boolean accepted, ReviewValidationFailure reason) { }
    private enum ReviewValidationFailure {
        NONE, ARTIFACT_REFERENCE_INVALID, FINDING_ARTIFACT_INVALID, APPROVAL_EVIDENCE_INCOMPLETE,
        FEEDBACK_UNSUBSTANTIATED, EVIDENCE_UNAVAILABLE, EVIDENCE_DIGEST_MISMATCH,
        EVIDENCE_LINEAGE_INVALID, IMPLEMENTATION_FINGERPRINT_MISMATCH, DECISION_CONTRADICTORY
    }
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
