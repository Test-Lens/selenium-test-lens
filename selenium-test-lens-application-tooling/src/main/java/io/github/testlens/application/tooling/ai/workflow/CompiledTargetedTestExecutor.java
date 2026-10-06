package io.github.testlens.application.tooling.ai.workflow;

/**
 * Targeted execution boundary that receives the exact immutable bytecode produced by the successful compile step.
 * @since 0.5.0
 */
@FunctionalInterface
public interface CompiledTargetedTestExecutor {
    TargetedTestExecutor.ExecutionResult execute(TargetedTestExecutor.ExecutionRequest request,
                                                 TargetedJavaCompiler.CompiledOutput compiledOutput) throws Exception;
}
