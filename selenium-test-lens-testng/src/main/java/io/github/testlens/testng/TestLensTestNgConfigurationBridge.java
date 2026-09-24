package io.github.testlens.testng;

import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

/** Internal bridge that exposes TestNG's owning test method before a user BeforeMethod executes. */
abstract class TestLensTestNgConfigurationBridge implements IConfigurationListener, ITestListener {
    @Override
    public final void beforeConfiguration(ITestResult configurationResult, ITestNGMethod testMethod) {
        if (configurationResult != null && testMethod != null
                && configurationResult.getMethod() != null
                && configurationResult.getMethod().isBeforeMethodConfiguration()) {
            beforeManagedMethodConfiguration(configurationResult, testMethod);
        }
    }

    @Override
    public final void onConfigurationSuccess(ITestResult configurationResult, ITestNGMethod testMethod) {
        afterManagedConfiguration(configurationResult, testMethod);
    }

    @Override
    public final void onConfigurationSuccess(ITestResult configurationResult) {
        afterManagedConfiguration(configurationResult, null);
    }

    @Override
    public final void onConfigurationFailure(ITestResult configurationResult, ITestNGMethod testMethod) {
        afterManagedConfiguration(configurationResult, testMethod);
    }

    @Override
    public final void onConfigurationFailure(ITestResult configurationResult) {
        afterManagedConfiguration(configurationResult, null);
    }

    @Override
    public final void onConfigurationSkip(ITestResult configurationResult, ITestNGMethod testMethod) {
        afterManagedConfiguration(configurationResult, testMethod);
    }

    @Override
    public final void onConfigurationSkip(ITestResult configurationResult) {
        afterManagedConfiguration(configurationResult, null);
    }

    @Override
    public final void onTestSuccess(ITestResult result) {
        afterManagedTestResult(result);
    }

    @Override
    public final void onTestFailure(ITestResult result) {
        afterManagedTestResult(result);
    }

    @Override
    public final void onTestSkipped(ITestResult result) {
        afterManagedTestResult(result);
    }

    @Override
    public final void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        afterManagedTestResult(result);
    }

    abstract void beforeManagedMethodConfiguration(ITestResult configurationResult, ITestNGMethod testMethod);

    abstract void afterManagedConfiguration(ITestResult configurationResult, ITestNGMethod testMethod);

    abstract void afterManagedTestResult(ITestResult result);
}
