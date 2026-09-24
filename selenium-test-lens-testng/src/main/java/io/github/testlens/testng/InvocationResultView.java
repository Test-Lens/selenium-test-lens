package io.github.testlens.testng;

import org.testng.IClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/** Read-only result view for a test invocation whose BeforeMethod envelope has started before TestNG publishes its result. */
final class InvocationResultView implements ITestResult {
    private final ITestNGMethod method;
    private final ITestContext context;
    private final Object instance;
    private final long startedAt;
    private final String id = UUID.randomUUID().toString();

    InvocationResultView(ITestNGMethod method, ITestContext context, Object instance) {
        this.method = method;
        this.context = context;
        this.instance = instance;
        this.startedAt = System.currentTimeMillis();
    }

    @Override public int getStatus() { return STARTED; }
    @Override public ITestNGMethod getMethod() { return method; }
    @Override public Object[] getParameters() { return new Object[0]; }
    @Override public IClass getTestClass() { return method.getTestClass(); }
    @Override public Throwable getThrowable() { return null; }
    @Override public long getStartMillis() { return startedAt; }
    @Override public long getEndMillis() { return 0; }
    @Override public String getName() { return method.getMethodName(); }
    @Override public boolean isSuccess() { return false; }
    @Override public String getHost() { return context == null ? null : context.getHost(); }
    @Override public Object getInstance() { return instance; }
    @Override public Object[] getFactoryParameters() { return new Object[0]; }
    @Override public String getTestName() { return context == null ? null : context.getName(); }
    @Override public String getInstanceName() { return instance == null ? null : instance.getClass().getName(); }
    @Override public ITestContext getTestContext() { return context; }
    @Override public boolean wasRetried() { return false; }
    @Override public String id() { return id; }
    @Override public Object getAttribute(String name) { return null; }
    @Override public Set<String> getAttributeNames() { return Collections.emptySet(); }
    @Override public Object removeAttribute(String name) { return null; }
    @Override public void setAttribute(String name, Object value) { throw readOnly(); }
    @Override public void setStatus(int status) { throw readOnly(); }
    @Override public void setParameters(Object[] parameters) { throw readOnly(); }
    @Override public void setThrowable(Throwable throwable) { throw readOnly(); }
    @Override public void setEndMillis(long millis) { throw readOnly(); }
    @Override public void setTestName(String name) { throw readOnly(); }
    @Override public void setWasRetried(boolean wasRetried) { throw readOnly(); }
    @Override public int compareTo(ITestResult other) { return id.compareTo(other == null ? "" : other.id()); }

    private static UnsupportedOperationException readOnly() {
        return new UnsupportedOperationException("The PER_CLASS pre-method invocation descriptor is read-only");
    }
}
