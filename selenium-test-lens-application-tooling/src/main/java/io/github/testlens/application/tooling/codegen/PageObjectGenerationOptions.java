package io.github.testlens.application.tooling.codegen;

import java.nio.file.Path;

/** Deterministic Page Object generation settings. @since 0.5.0 */
public record PageObjectGenerationOptions(String packageName, Path outputDirectory,
                                          ReviewSelectorPolicy reviewSelectorPolicy,
                                          boolean createUserExtensions) {
    public PageObjectGenerationOptions {
        if (packageName == null || !packageName.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")
                || java.util.Arrays.stream(packageName.split("\\.")).anyMatch(JAVA_KEYWORDS::contains))
            throw new IllegalArgumentException("packageName must be a valid Java package");
        if (outputDirectory == null) throw new IllegalArgumentException("outputDirectory is required");
        if (reviewSelectorPolicy == null) reviewSelectorPolicy = ReviewSelectorPolicy.SKIP;
    }
    public static PageObjectGenerationOptions verifiedOnly(String packageName, Path outputDirectory) {
        return new PageObjectGenerationOptions(packageName, outputDirectory, ReviewSelectorPolicy.SKIP, true);
    }
    public enum ReviewSelectorPolicy { SKIP, GENERATE_WITH_WARNING }
    private static final java.util.Set<String> JAVA_KEYWORDS=java.util.Set.of("abstract","assert","boolean","break","byte","case","catch","char","class","const","continue","default","do","double","else","enum","extends","final","finally","float","for","goto","if","implements","import","instanceof","int","interface","long","native","new","package","private","protected","public","return","short","static","strictfp","super","switch","synchronized","this","throw","throws","transient","try","void","volatile","while","record","sealed","permits","non-sealed","var","yield","true","false","null");
}
