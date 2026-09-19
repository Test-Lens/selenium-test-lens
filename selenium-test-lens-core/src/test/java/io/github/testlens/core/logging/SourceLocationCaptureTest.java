package io.github.testlens.core.logging;

import org.junit.jupiter.api.Test;

import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SourceLocationCaptureTest {
    @Test void selectsFirstConsumerAfterTestLensSeleniumAndReflectionFrames() {
        SourceLocation location = SourceLocationCapture.select(List.of(
                frame("io.github.testlens.actions.SmartClickActions", "click", "SmartClickActions.java", 147),
                frame("io.github.testlens.selenium.locator.UiLocator", "click", "UiLocator.java", 91),
                frame("org.openqa.selenium.remote.RemoteWebElement", "click", "RemoteWebElement.java", 77),
                frame("java.lang.reflect.Method", "invoke", "Method.java", 580),
                frame("example.pages.LoginPage", "login", "LoginPage.java", 53),
                frame("example.EonPlTest", "testLogin", "EonPlTest.java", 203))).orElseThrow();

        assertEquals("example.pages.LoginPage", location.className());
        assertEquals("login", location.methodName());
        assertEquals("LoginPage.java", location.fileName());
        assertEquals(53, location.lineNumber());
        assertEquals("LoginPage.java:53", location.displayName());
    }

    @Test void supportsDirectTestCallAndSkipsJUnitTestNgAndProxyPlumbing() {
        SourceLocation location = SourceLocationCapture.select(List.of(
                frame("org.junit.jupiter.engine.execution.MethodInvocation", "proceed", "MethodInvocation.java", 60),
                frame("org.testng.internal.invokers.MethodInvocationHelper", "invokeMethod", "MethodInvocationHelper.java", 130),
                frame("com.sun.proxy.$Proxy12", "click", "$Proxy12.java", 1),
                frame("consumer.LoginTest", "logsIn", "LoginTest.java", 81))).orElseThrow();
        assertEquals(new SourceLocation("consumer.LoginTest", "logsIn", "LoginTest.java", 81), location);
    }

    @Test void ignoresFramesWithoutUsefulSourceInformation() {
        assertTrue(SourceLocationCapture.select(List.of(frame("consumer.Native", "call", null, -2))).isEmpty());
    }

    @Test void similarlyNamedConsumerPackageIsNotFiltered() {
        SourceLocation location = SourceLocationCapture.select(List.of(
                frame("io.github.testlens.consumer.pages.LoginPage", "login", "LoginPage.java", 44)))
                .orElseThrow();
        assertEquals("io.github.testlens.consumer.pages.LoginPage", location.className());
    }

    @Test void loggerDoesNotWalkStackUnlessCaptureWasRequested() {
        SourceLocationCapture.resetCaptureInvocations();
        AtomicReference<UiTestLensLogEntry> emitted = new AtomicReference<>();
        UiTestLensLogger logger = UiTestLensLogger.builder().sink(emitted::set).build();

        logger.emit(UiTestLensLogEntry.info("disabled"));

        assertEquals(0, SourceLocationCapture.captureInvocations());
        assertTrue(emitted.get().sourceLocation().isEmpty());
        assertFalse(emitted.get().metadata().containsKey(SourceLocationCapture.CAPTURE_MARKER));

        logger.emit(UiTestLensLogEntry.builder().message("enabled")
                .metadata(SourceLocationCapture.CAPTURE_MARKER, "true").build());

        assertEquals(1, SourceLocationCapture.captureInvocations());
        assertFalse(emitted.get().metadata().containsKey(SourceLocationCapture.CAPTURE_MARKER));
    }

    private static StackFrame frame(String className, String method, String file, int line) {
        return new StackFrame() {
            public String getClassName() { return className; }
            public String getMethodName() { return method; }
            public Class<?> getDeclaringClass() { return Object.class; }
            public int getByteCodeIndex() { return 0; }
            public String getFileName() { return file; }
            public int getLineNumber() { return line; }
            public boolean isNativeMethod() { return false; }
            public StackTraceElement toStackTraceElement() { return new StackTraceElement(className, method, file, line); }
            public String getDescriptor() { return "()V"; }
            public MethodType getMethodType() { return MethodType.methodType(void.class); }
        };
    }
}
