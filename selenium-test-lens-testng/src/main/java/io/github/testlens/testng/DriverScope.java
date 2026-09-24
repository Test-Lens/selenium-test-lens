package io.github.testlens.testng;

/** Defines the WebDriver ownership lifetime used by the managed TestNG adapter. @since 0.4.0 */
public enum DriverScope {
    /** Creates and closes one driver for every physical TestNG test invocation. */
    PER_METHOD,
    /** Reuses one driver for sequential invocations of one concrete TestNG class instance. */
    PER_CLASS
}
