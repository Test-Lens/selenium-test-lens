package io.github.testlens.studio.browser;

/**
 * Explicit, synchronous boundary for browser availability and creation.
 * Implementations used for targeted test execution must expose a public no-argument constructor so the
 * isolated test JVM can reconstruct the provider without transferring browser state or credentials.
 *
 * @since 0.5.0
 */
public interface BrowserSessionProvider {
    BrowserAvailability preflight(BrowserRequest request);

    BrowserSession open(BrowserRequest request);
}
