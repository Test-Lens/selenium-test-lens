package io.github.testlens.testng;

import io.github.testlens.selenium.execution.HeadlessMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Configures the managed factory and driver lifetime used by {@link TestLensTestNgListener}. */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestLensTestNg {
    /**
     * Factory type instantiated through a public no-argument constructor for every physical invocation.
     *
     * @return the invocation factory type
     */
    Class<? extends TestLensTestNgFactory> factory();

    /**
     * Selects the driver ownership lifetime. The default preserves the original per-invocation behavior.
     *
     * @return configured driver scope
     * @since 0.4.0
     */
    DriverScope driverScope() default DriverScope.PER_METHOD;

    /**
     * @return explicit intent, or UNSET to resolve process configuration
     * @since 0.4.0
     */
    HeadlessMode headless() default HeadlessMode.UNSET;
}
