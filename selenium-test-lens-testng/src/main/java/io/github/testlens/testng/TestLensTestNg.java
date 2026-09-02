package io.github.testlens.testng;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Configures the per-invocation factory used by {@link TestLensTestNgListener}. */
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
}
