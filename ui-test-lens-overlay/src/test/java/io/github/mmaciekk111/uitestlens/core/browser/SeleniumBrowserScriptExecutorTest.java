package io.github.mmaciekk111.uitestlens.core.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SeleniumBrowserScriptExecutorTest {

    @Test
    void executeDelegatesToSeleniumExecuteScript() {
        FakeJavascriptExecutor fake = new FakeJavascriptExecutor();
        SeleniumBrowserScriptExecutor executor = new SeleniumBrowserScriptExecutor(fake);

        Object result = executor.execute("return arguments[0];", "value", 7);

        assertEquals("sync-result", result);
        assertEquals("return arguments[0];", fake.lastScript);
        assertArrayEquals(new Object[]{"value", 7}, fake.lastArgs);
        assertEquals(1, fake.executeCalls);
    }

    @Test
    void executeAsyncDelegatesToSeleniumExecuteAsyncScript() {
        FakeJavascriptExecutor fake = new FakeJavascriptExecutor();
        SeleniumBrowserScriptExecutor executor = new SeleniumBrowserScriptExecutor(fake);

        Object result = executor.executeAsync("arguments[0]();", "done");

        assertEquals("async-result", result);
        assertEquals("arguments[0]();", fake.lastAsyncScript);
        assertArrayEquals(new Object[]{"done"}, fake.lastAsyncArgs);
        assertEquals(1, fake.executeAsyncCalls);
    }

    @Test
    void rejectsNullJavascriptExecutor() {
        assertThrows(IllegalArgumentException.class, () -> new SeleniumBrowserScriptExecutor((JavascriptExecutor) null));
    }

    @Test
    void rejectsWebDriverWithoutJavascriptExecutor() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> null
        );

        assertThrows(IllegalArgumentException.class, () -> new SeleniumBrowserScriptExecutor(driver));
    }

    @Test
    void defaultAsyncExecutionIsUnsupportedForNeutralExecutor() {
        BrowserScriptExecutor executor = (script, args) -> null;

        assertThrows(UnsupportedOperationException.class, () -> executor.executeAsync("async"));
    }

    private static final class FakeJavascriptExecutor implements JavascriptExecutor {
        private String lastScript;
        private Object[] lastArgs;
        private String lastAsyncScript;
        private Object[] lastAsyncArgs;
        private int executeCalls;
        private int executeAsyncCalls;

        @Override
        public Object executeScript(String script, Object... args) {
            executeCalls++;
            lastScript = script;
            lastArgs = Arrays.copyOf(args, args.length);
            return "sync-result";
        }

        @Override
        public Object executeAsyncScript(String script, Object... args) {
            executeAsyncCalls++;
            lastAsyncScript = script;
            lastAsyncArgs = Arrays.copyOf(args, args.length);
            return "async-result";
        }
    }
}
