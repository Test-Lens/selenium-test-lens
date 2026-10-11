package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.core.redaction.RedactionPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.testlens.application.tooling.ai.workflow.AgentExecutor.AgentFailureCode;
import static io.github.testlens.application.tooling.ai.workflow.AgentExecutor.AgentFailureDiagnostic;

/**
 * Bounded subprocess implementation of the provider-neutral {@link AgentExecutor} boundary.
 * The child runs in an isolated staging directory and receives task data only through stdin.
 * @since 0.5.0
 */
public final class ExternalAgentRunner implements AgentExecutor {
    private static final Duration CLEANUP_TIMEOUT = Duration.ofSeconds(2);
    private final AgentProfile profile;
    private final Path stagingRoot;
    private final AgentProtocolCodec codec;
    private final RedactionPolicy redaction;
    private final List<String> canaries;
    private final List<SourceExcerpt> excerpts;
    private final Map<String, String> hostEnvironment;
    private final java.util.function.Consumer<ExecutionMetrics> metricsSink;
    private final AgentArtifactSecurityGate securityGate = new AgentArtifactSecurityGate();

    public ExternalAgentRunner(AgentProfile profile, Path stagingRoot) {
        this(profile, stagingRoot, new AgentProtocolCodec(), RedactionPolicy.defaults(), List.of(), List.of(),
                System.getenv(), ignored -> { });
    }

    public ExternalAgentRunner(AgentProfile profile, Path stagingRoot, AgentProtocolCodec codec,
                               RedactionPolicy redaction, List<String> canaries,
                               List<SourceExcerpt> excerpts, Map<String, String> hostEnvironment) {
        this(profile, stagingRoot, codec, redaction, canaries, excerpts, hostEnvironment, ignored -> { });
    }

    public ExternalAgentRunner(AgentProfile profile, Path stagingRoot, AgentProtocolCodec codec,
                               RedactionPolicy redaction, List<String> canaries,
                               List<SourceExcerpt> excerpts, Map<String, String> hostEnvironment,
                               java.util.function.Consumer<ExecutionMetrics> metricsSink) {
        this.profile = java.util.Objects.requireNonNull(profile, "profile");
        if (stagingRoot == null || !stagingRoot.isAbsolute()) {
            throw new IllegalArgumentException("stagingRoot must be absolute");
        }
        this.stagingRoot = stagingRoot.normalize();
        this.codec = java.util.Objects.requireNonNull(codec, "codec");
        this.redaction = java.util.Objects.requireNonNull(redaction, "redaction");
        this.canaries = List.copyOf(canaries == null ? List.of() : canaries);
        this.excerpts = List.copyOf(excerpts == null ? List.of() : excerpts);
        this.hostEnvironment = Map.copyOf(hostEnvironment == null ? Map.of() : hostEnvironment);
        this.metricsSink = java.util.Objects.requireNonNull(metricsSink, "metricsSink");
    }

    @Override
    public AgentResult execute(AgentCommand command) throws AgentExecutionException {
        FailureTrace trace = new FailureTrace();
        trace.startedNanos = System.nanoTime();
        try {
            return execute(command, trace);
        } catch (AgentExecutionException failure) {
            throw augmented(failure, command, trace);
        }
    }

    private AgentResult execute(AgentCommand command, FailureTrace trace) throws AgentExecutionException {
        if (command.role() != profile.role()) {
            throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED,
                    "Agent profile role does not match command role", null,
                    "ROLE_PROFILE_MISMATCH", "command.role");
        }
        PageObjectApiReferenceValidator pageObjectApiValidator = PageObjectApiReferenceValidator.from(command);
        byte[] input;
        byte[] schema;
        try {
            Map<String, String> externalInstructions = new java.util.TreeMap<>(command.instructions());
            if (!excerpts.isEmpty()) {
                externalInstructions.put("testLens.sourceExcerptPaths", excerpts.stream()
                        .map(value -> "sources/" + value.logicalPath().toString().replace('\\', '/'))
                        .sorted().collect(java.util.stream.Collectors.joining(",")));
            }
            AgentCommand externalCommand = new AgentCommand(command.runId(), command.role(), command.inputs(),
                    externalInstructions);
            input = codec.encodeCommand(externalCommand);
            schema = codec.outputSchema(command.role());
            trace.inputBytes = input.length;
        } catch (RuntimeException invalid) {
            throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED, "Agent command cannot be serialized", invalid,
                    "COMMAND_SERIALIZATION_FAILED", "command");
        }
        if (input.length > profile.maxInputBytes()) {
            throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED, "Agent command exceeds maxInputBytes", null,
                    "INPUT_OVERSIZE", "command");
        }
        requireSafe(input, command.inputs().stream().map(ArtifactEnvelope::artifactId).toList(), true);

        long startedNanos = System.nanoTime();
        int outputBytes = 0;
        ExecutionStatus executionStatus = ExecutionStatus.FAILED;
        Path stage = null;
        try {
            Path root = prepareRoot();
            stage = Files.createTempDirectory(root, "test-lens-agent-");
            restrictDirectory(stage);
            Path stageReal = stage.toRealPath();
            writeExcerpts(stageReal);
            Path schemaFile = safeFile(stageReal, "result.schema.json", schema);
            Path outputFile = safeFile(stageReal, "result.json", new byte[0]);
            List<String> processCommand = commandLine(stageReal, schemaFile, outputFile);

            ProcessBuilder builder = new ProcessBuilder(processCommand).directory(stageReal.toFile());
            Map<String, String> environment = builder.environment();
            environment.clear();
            for (String name : profile.environmentAllowlist()) {
                String value = hostEnvironment.get(name);
                if (value != null) environment.put(name, value);
            }

            Process process;
            try {
                trace.processStatus = "STARTING";
                process = builder.start();
            } catch (IOException unavailable) {
                trace.processStatus = "START_FAILED";
                throw failure(AgentFailureCode.AGENT_NOT_AVAILABLE, "External agent process could not be started",
                        unavailable, "PROCESS_START_FAILED", "process.start");
            }
            trace.processStatus = "RUNNING";
            ProcessOutcome outcome = communicate(process, input);
            trace.exitCode = outcome.timedOut() ? null : outcome.exitCode();
            trace.processStatus = outcome.timedOut() ? "TIMED_OUT" : "EXITED";
            if (outcome.timedOut()) {
                throw failure(AgentFailureCode.AGENT_TIMEOUT, "External agent exceeded its timeout", null,
                        "PROCESS_TIMEOUT", "process.wait");
            }
            if (outcome.diagnosticsOverflow()) {
                throw failure(AgentFailureCode.AGENT_PROCESS_FAILED,
                        "External agent diagnostics exceeded their bound", null,
                        "DIAGNOSTIC_OVERFLOW", "process.diagnostics");
            }
            if (outcome.exitCode() != 0) {
                throw failure(AgentFailureCode.AGENT_PROCESS_FAILED,
                        "External agent exited with a non-zero code", null,
                        "PROCESS_NONZERO_EXIT", "process.exit");
            }

            byte[] output = switch (profile.outputTransport()) {
                case STDOUT -> {
                    trace.outputBytes = outcome.stdoutBytes();
                    if (outcome.stdoutOverflow()) {
                        throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID,
                                "External agent output exceeded maxOutputBytes", null,
                                "OUTPUT_OVERSIZE", "result");
                    }
                    yield outcome.stdout();
                }
                case FILE -> readOutput(stageReal, outputFile, trace);
            };
            outputBytes = output.length;
            trace.outputBytes = output.length;
            if (output.length == 0) {
                throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID, "External agent returned empty output", null,
                        "OUTPUT_EMPTY", "result");
            }
            if (command.role() == Role.TEST_IMPLEMENTER) {
                requireStructuredImplementerOutputSafe(output, pageObjectApiValidator);
            } else {
                requireSafe(output, List.of(), false);
            }
            AgentProtocolCodec.DecodedResult decoded;
            try {
                decoded = codec.decode(command.role(), output);
            } catch (RuntimeException invalid) {
                AgentProtocolCodec.DecodeFailure decodeFailure = AgentProtocolCodec.decodeFailure(invalid);
                String reason = decodeFailure.name();
                if (decodeFailure == AgentProtocolCodec.DecodeFailure.MALFORMED_JSON
                        && likelyTruncatedJson(output)) {
                    reason = "TRUNCATED_JSON";
                }
                String location = AgentProtocolCodec.decodeLocation(invalid);
                throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID,
                        "External agent output is invalid at " + location + ": "
                                + AgentProtocolCodec.decodeDetail(invalid), null, reason, location);
            }
            Object acceptedPayload = decoded.payload();
            if (decoded.payload() instanceof TestImplementationProposal implementation) {
                PageObjectApiReferenceValidator.SourceVerification verification =
                        pageObjectApiValidator.verifySource(implementation.sourcePatch(),
                                implementation.pageObjectApisUsed());
                if (!verification.valid()) {
                    String validationLocation = decoded.payloadBasePath() + ".sourcePatch";
                    throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID,
                            "External agent output contains unverifiable Page Object API usage", null,
                            verification.reason(), validationLocation, verification.safeMetadata());
                }
                acceptedPayload = new TestImplementationProposal(implementation.header(), implementation.scenarioId(),
                        implementation.sourcePatch(), implementation.selectorAccessPolicy(),
                        verification.verifiedReferences(),
                        implementation.missingCapabilities());
            }
            String digest = ArtifactEnvelope.digest(output);
            int attempt = command.inputs().stream().mapToInt(ArtifactEnvelope::attempt).max().orElse(0) + 1;
            List<String> parents = command.inputs().stream().map(ArtifactEnvelope::artifactId).distinct().sorted().toList();
            String artifactId = "agent-" + command.role().name().toLowerCase(java.util.Locale.ROOT)
                    + "-" + digest.substring(0, 16);
            ArtifactEnvelope<?> artifact = ArtifactEnvelope.create(artifactId, command.runId(), parents, attempt,
                    acceptedPayload);
            executionStatus = ExecutionStatus.SUCCESS;
            return new AgentResult(profile.id() + ":" + digest.substring(0, 16), artifact);
        } catch (AgentExecutionException expected) {
            executionStatus = expected.code() == AgentFailureCode.AGENT_TIMEOUT
                    ? ExecutionStatus.TIMEOUT : ExecutionStatus.FAILED;
            throw expected;
        } catch (IOException failure) {
            throw failure(AgentFailureCode.AGENT_PROCESS_FAILED, "External agent I/O failed", failure,
                    "PROCESS_IO_FAILED", "process.io");
        } finally {
            if (stage != null) {
                try {
                    deleteTree(stage);
                } catch (IOException cleanupFailure) {
                    // Cleanup failures are deliberately not logged because staging files may contain agent output.
                }
            }
            try {
                metricsSink.accept(new ExecutionMetrics(profile.id(), command.role(), executionStatus,
                        input.length, outputBytes, Duration.ofNanos(System.nanoTime() - startedNanos).toMillis()));
            } catch (RuntimeException ignored) {
                // Observability must not change the external execution result.
            }
        }
    }

    private Path prepareRoot() throws IOException, AgentExecutionException {
        if (Files.exists(stagingRoot, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(stagingRoot)) {
            throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED, "stagingRoot must not be a symbolic link", null);
        }
        Files.createDirectories(stagingRoot);
        Path real = stagingRoot.toRealPath();
        if (!Files.isDirectory(real)) {
            throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED, "stagingRoot must be a directory", null);
        }
        return real;
    }

    private void writeExcerpts(Path stage) throws IOException, AgentExecutionException {
        if (excerpts.isEmpty()) return;
        Path sourceRoot = stage.resolve("sources");
        Files.createDirectory(sourceRoot);
        restrictDirectory(sourceRoot);
        for (SourceExcerpt excerpt : excerpts) {
            requireSafe(excerpt.content().getBytes(StandardCharsets.UTF_8), List.of(excerpt.logicalPath().toString()), true);
            if (!ArtifactEnvelope.digest(excerpt.content()).equals(excerpt.contentFingerprint())) {
                throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED, "Source excerpt fingerprint does not match", null);
            }
            Path target = sourceRoot.resolve(excerpt.logicalPath()).normalize();
            if (!target.startsWith(sourceRoot) || Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw failure(AgentFailureCode.AGENT_CONTEXT_REJECTED, "Source excerpt path is not isolated", null);
            }
            Files.createDirectories(target.getParent());
            Files.writeString(target, excerpt.content(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
    }

    private List<String> commandLine(Path workingDirectory, Path schemaFile, Path outputFile)
            throws AgentExecutionException, IOException {
        if (!Files.exists(profile.executable(), LinkOption.NOFOLLOW_LINKS)
                || !Files.isRegularFile(profile.executable(), LinkOption.NOFOLLOW_LINKS)) {
            throw failure(AgentFailureCode.AGENT_NOT_AVAILABLE, "Configured agent executable is unavailable", null,
                    "PROCESS_START_FAILED", "process.executable");
        }
        Path executable = profile.executable().toRealPath();
        List<String> values = new ArrayList<>();
        values.add(executable.toString());
        for (String argument : profile.arguments()) {
            values.add(argument.replace(AgentProfile.SCHEMA_FILE, schemaFile.toString())
                    .replace(AgentProfile.OUTPUT_FILE, outputFile.toString())
                    .replace(AgentProfile.WORKING_DIRECTORY, workingDirectory.toString()));
        }
        return List.copyOf(values);
    }

    private ProcessOutcome communicate(Process process, byte[] input) throws AgentExecutionException {
        ExecutorService streams = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "test-lens-external-agent-io");
            thread.setDaemon(true);
            return thread;
        });
        int stdoutLimit = profile.outputTransport() == AgentProfile.OutputTransport.STDOUT
                ? profile.maxOutputBytes() : profile.maxDiagnosticsBytes();
        Future<BoundedBytes> stdout = streams.submit(() -> drain(process.getInputStream(), stdoutLimit));
        Future<BoundedBytes> stderr = streams.submit(() -> drain(process.getErrorStream(), profile.maxDiagnosticsBytes()));
        Future<?> writer = streams.submit(() -> {
            write(process.getOutputStream(), input);
            return null;
        });
        boolean finished;
        try {
            finished = process.waitFor(profile.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) terminate(process);
            await(writer);
            BoundedBytes out = await(stdout);
            BoundedBytes error = await(stderr);
            return new ProcessOutcome(finished ? process.exitValue() : -1, !finished, out.bytes(), out.overflow(),
                    error.overflow() || (profile.outputTransport() == AgentProfile.OutputTransport.FILE && out.overflow()),
                    out.totalBytes());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            terminate(process);
            throw failure(AgentFailureCode.AGENT_PROCESS_FAILED, "External agent execution was interrupted", interrupted);
        } finally {
            streams.shutdownNow();
        }
    }

    private byte[] readOutput(Path stage, Path outputFile, FailureTrace trace)
            throws IOException, AgentExecutionException {
        if (!Files.exists(outputFile, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(outputFile)
                || !outputFile.toRealPath().startsWith(stage) || !Files.isRegularFile(outputFile, LinkOption.NOFOLLOW_LINKS)) {
            throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID, "External agent output path is unsafe", null);
        }
        long size = Files.size(outputFile);
        trace.outputBytes = size;
        if (size < 1) {
            throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID, "External agent returned empty output", null,
                    "OUTPUT_EMPTY", "result");
        }
        if (size > profile.maxOutputBytes()) {
            throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID, "External agent output exceeded maxOutputBytes", null,
                    "OUTPUT_OVERSIZE", "result");
        }
        return Files.readAllBytes(outputFile);
    }

    private void requireSafe(byte[] bytes, List<String> references, boolean input) throws AgentExecutionException {
        String text = new String(bytes, StandardCharsets.UTF_8);
        var result = securityGate.validate(text, references, redaction, canaries);
        if (result.status() != AgentArtifactSecurityGate.Status.PASS) {
            List<String> codes = result.findings().stream().map(AgentArtifactSecurityGate.Finding::code)
                    .distinct().sorted().toList();
            String reason = codes.size() == 1 ? codes.get(0) : "MULTIPLE_SECURITY_FINDINGS";
            String location = result.findings().size() == 1
                    && !"text".equals(result.findings().get(0).location())
                    ? result.findings().get(0).location() : input ? "command" : "result";
            throw failure(input ? AgentFailureCode.AGENT_CONTEXT_REJECTED : AgentFailureCode.AGENT_OUTPUT_INVALID,
                    (input ? "External agent context" : "External agent output") + " failed the security gate", null,
                    reason, location);
        }
    }

    private void requireStructuredImplementerOutputSafe(byte[] output,
            PageObjectApiReferenceValidator pageObjectApiValidator) throws AgentExecutionException {
        var result = securityGate.validateStructuredAgentOutput(
                new String(output, StandardCharsets.UTF_8), redaction, canaries,
                Set.of("$.payload.sourcePatch", "$.result.payload.sourcePatch"),
                pageObjectApiValidator::isTrustedStructuredValue);
        if (result.status() == AgentArtifactSecurityGate.Status.PASS) return;
        List<String> codes = result.findings().stream().map(AgentArtifactSecurityGate.Finding::code)
                .distinct().sorted().toList();
        String reason = codes.size() == 1 ? codes.get(0) : "MULTIPLE_SECURITY_FINDINGS";
        String location = result.findings().size() == 1 ? result.findings().get(0).location() : "$.multiple";
        throw failure(AgentFailureCode.AGENT_OUTPUT_INVALID,
                "External agent output failed the security gate", null, reason, location);
    }

    private static Path safeFile(Path stage, String name, byte[] content) throws IOException {
        Path target = stage.resolve(name).normalize();
        if (!target.startsWith(stage) || Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Unsafe staging file " + name);
        }
        Files.write(target, content, StandardOpenOption.CREATE_NEW);
        return target;
    }

    private static BoundedBytes drain(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(limit, 8_192));
        byte[] buffer = new byte[8_192];
        int read;
        long total = 0;
        boolean overflow = false;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            int remaining = limit - output.size();
            if (remaining > 0) output.write(buffer, 0, Math.min(read, remaining));
            if (read > remaining) overflow = true;
        }
        return new BoundedBytes(output.toByteArray(), overflow, total);
    }

    private static void write(OutputStream output, byte[] bytes) throws IOException {
        try (output) {
            output.write(bytes);
            output.flush();
        }
    }

    private static <T> T await(Future<T> future) throws AgentExecutionException {
        try {
            return future.get(CLEANUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw failure(AgentFailureCode.AGENT_PROCESS_FAILED, "External agent stream handling was interrupted", interrupted);
        } catch (ExecutionException | TimeoutException failure) {
            throw failure(AgentFailureCode.AGENT_PROCESS_FAILED, "External agent stream handling failed", failure);
        }
    }

    private static void terminate(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(CLEANUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor(CLEANUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static void restrictDirectory(Path directory) {
        try {
            Files.setPosixFilePermissions(directory, Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // Windows ACL inheritance and non-POSIX file systems remain host policy responsibilities.
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static boolean likelyTruncatedJson(byte[] output) {
        String text = new String(output, StandardCharsets.UTF_8).strip();
        if (!text.startsWith("{")) return false;
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (quoted) {
                if (escaped) escaped = false;
                else if (character == '\\') escaped = true;
                else if (character == '"') quoted = false;
                continue;
            }
            if (character == '"') quoted = true;
            else if (character == '{' || character == '[') depth++;
            else if (character == '}' || character == ']') {
                depth--;
                if (depth < 0) return false;
            }
        }
        return quoted || escaped || depth > 0;
    }

    private AgentExecutionException augmented(AgentExecutionException failure, AgentCommand command,
                                              FailureTrace trace) {
        AgentFailureDiagnostic original = failure.diagnostic();
        String reason = original == null ? "BOUNDARY_FAILURE" : original.reasonCode();
        String location = original == null ? "boundary" : original.location();
        Map<String, String> metadata = new java.util.TreeMap<>();
        if (original != null) metadata.putAll(original.metadata());
        metadata.put("role", command == null || command.role() == null ? "UNKNOWN" : command.role().name());
        metadata.put("profileId", profile.id());
        metadata.put("inputBytes", Long.toString(trace.inputBytes));
        metadata.put("outputBytes", Long.toString(trace.outputBytes));
        metadata.put("durationMillis", Long.toString(
                Duration.ofNanos(System.nanoTime() - trace.startedNanos).toMillis()));
        metadata.put("processStatus", trace.processStatus);
        if (trace.exitCode != null) metadata.put("exitCode", Integer.toString(trace.exitCode));
        return new AgentExecutionException(failure.code(), failure.getMessage(), failure.getCause(),
                new AgentFailureDiagnostic(reason, location, metadata));
    }

    private static AgentExecutionException failure(AgentFailureCode code, String message, Throwable cause) {
        return failure(code, message, cause, "BOUNDARY_FAILURE", "boundary");
    }

    private static AgentExecutionException failure(AgentFailureCode code, String message, Throwable cause,
                                                   String reasonCode, String location) {
        return failure(code, message, cause, reasonCode, location, Map.of());
    }

    private static AgentExecutionException failure(AgentFailureCode code, String message, Throwable cause,
                                                   String reasonCode, String location,
                                                   Map<String, String> metadata) {
        AgentFailureDiagnostic diagnostic = new AgentFailureDiagnostic(reasonCode, location, metadata);
        return new AgentExecutionException(code, message, cause, diagnostic);
    }

    public record SourceExcerpt(Path logicalPath, String content, String contentFingerprint) {
        public SourceExcerpt {
            if (logicalPath == null || logicalPath.isAbsolute() || logicalPath.normalize().startsWith("..")
                    || content == null || contentFingerprint == null || contentFingerprint.isBlank()) {
                throw new IllegalArgumentException("A relative excerpt path, content, and fingerprint are required");
            }
            logicalPath = logicalPath.normalize();
        }
    }

    public record ExecutionMetrics(String profileId, AgentExecutor.Role role, ExecutionStatus status,
                                   long inputBytes, long outputBytes, long durationMillis) {
        public ExecutionMetrics {
            if (profileId == null || profileId.isBlank() || role == null || status == null
                    || inputBytes < 0 || outputBytes < 0 || durationMillis < 0) {
                throw new IllegalArgumentException("valid non-sensitive execution metrics are required");
            }
        }
    }

    public enum ExecutionStatus { SUCCESS, FAILED, TIMEOUT }

    private record BoundedBytes(byte[] bytes, boolean overflow, long totalBytes) { }
    private record ProcessOutcome(int exitCode, boolean timedOut, byte[] stdout, boolean stdoutOverflow,
                                  boolean diagnosticsOverflow, long stdoutBytes) { }

    private static final class FailureTrace {
        private long startedNanos;
        private long inputBytes;
        private long outputBytes;
        private String processStatus = "NOT_STARTED";
        private Integer exitCode;
    }
}
