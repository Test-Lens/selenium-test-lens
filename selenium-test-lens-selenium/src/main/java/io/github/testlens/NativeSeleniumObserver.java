package io.github.testlens;

import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.decorators.Decorated;
import org.openqa.selenium.support.decorators.DefaultDecorated;
import org.openqa.selenium.support.decorators.WebDriverDecorator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Internal Selenium decorator. It observes calls but never changes their execution policy. */
final class NativeSeleniumObserver extends WebDriverDecorator<WebDriver> {
    private final JsOverlayDebug lens;
    private final WebDriver attachedDriver;
    private final ThreadLocal<Invocation> invocation = new ThreadLocal<>();
    private volatile WebDriver observedDriver;

    NativeSeleniumObserver(WebDriver driver, JsOverlayDebug lens) {
        super(WebDriver.class);
        this.attachedDriver = driver;
        this.lens = lens;
    }

    synchronized WebDriver observeDriver() {
        if (observedDriver == null) observedDriver = decorate(attachedDriver);
        return observedDriver;
    }

    WebElement observe(WebElement element, String label) {
        if (element == null) throw new IllegalArgumentException("element must not be null");
        ObservedElement existing = observedByThis(element);
        String effectiveLabel = label == null ? "" : label;
        if (existing != null && (effectiveLabel.isBlank() || effectiveLabel.equals(existing.metadata.label()))) {
            return element;
        }
        WebElement original = existing == null ? element : existing.getOriginal();
        validateOwnership(original);
        ElementMetadata metadata = existing == null ? ElementMetadata.unknown() : existing.metadata;
        if (!effectiveLabel.isBlank()) metadata = metadata.withLabel(effectiveLabel);
        return createProxy(new ObservedElement(original, metadata), WebElement.class);
    }

    @Override
    public Decorated<WebElement> createDecorated(WebElement original) {
        Invocation current = invocation.get();
        ElementMetadata metadata = current == null ? ElementMetadata.unknown() : current.resultMetadata();
        return new ObservedElement(original, metadata);
    }

    @Override
    public Object call(Decorated<?> target, Method method, Object[] args) throws Throwable {
        Invocation current = invocation.get();
        Object overlayToken = null;
        boolean overlayHidden = false;
        if (current != null && current.requiresOverlaySuppression() && lens.automaticFeedbackEnabled()) {
            try {
                overlayToken = lens.hideDebugArtifactsTemporarily();
                overlayHidden = true;
            } catch (RuntimeException ignored) {
                // The native Selenium command remains authoritative when diagnostics are unavailable.
            }
        }
        try {
            Object result = super.call(target, method, args);
            if ("getShadowRoot".equals(method.getName()) && result instanceof SearchContext shadow) {
                ElementMetadata parent = target instanceof ObservedElement observed
                        ? observed.metadata : ElementMetadata.unknown();
                return new ObservedShadowRoot(shadow, parent);
            }
            return result;
        } finally {
            if (overlayHidden) {
                try { lens.restoreDebugArtifacts(overlayToken); }
                catch (RuntimeException ignored) {
                    // Cleanup diagnostics cannot replace the Selenium result or trigger a retry.
                }
            }
        }
    }

    @Override
    public void beforeCall(Decorated<?> target, Method method, Object[] args) {
        Invocation current = Invocation.create(target, method, args, lens.observationActive());
        invocation.set(current);
        if (!current.observed()) return;
        if (current.kind() == Kind.ACTION) {
            lens.emitNativeOperation(current.action(), current.description(), UiTestLensStatus.STARTED,
                    UiTestLensLogLevel.INFO, current.target(), null);
            current.targets().forEach(element -> highlight(element, HighlightState.ACTION));
        }
    }

    @Override
    public void afterCall(Decorated<?> target, Method method, Object[] args, Object result) {
        Invocation current = invocation.get();
        try {
            if (current == null || !current.observed()) return;
            if (current.kind() == Kind.ACTION) {
                lens.emitNativeOperation(current.action(), current.description(), UiTestLensStatus.PASSED,
                        UiTestLensLogLevel.INFO, current.target(), null);
                current.targets().forEach(element -> highlight(element, HighlightState.SUCCESS));
            } else if (current.kind() == Kind.TECHNICAL) {
                lens.emitNativeTechnical(current.action(), current.description(), current.target(), null);
            }
        } finally {
            invocation.remove();
        }
    }

    @Override
    public Object onError(Decorated<?> target, Method method, Object[] args, InvocationTargetException error)
            throws Throwable {
        Invocation current = invocation.get();
        Throwable original = error.getTargetException();
        try {
            if (current != null && current.observed()) {
                if (current.kind() == Kind.ACTION) {
                    lens.emitNativeOperation(current.action(), current.description(), UiTestLensStatus.FAILED,
                            UiTestLensLogLevel.ERROR, current.target(), original);
                    current.targets().forEach(element -> highlight(element, HighlightState.FAILURE));
                } else if (current.kind() == Kind.TECHNICAL) {
                    // Polling/read failures remain technical diagnostics, never functional failures.
                    lens.emitNativeTechnical(current.action(), current.description(), current.target(), original);
                }
            }
        } finally {
            invocation.remove();
        }
        throw original;
    }

    private void highlight(ElementTarget element, HighlightState state) {
        try {
            lens.automaticHighlight(element.original(),
                    lens.redactObservationLabel(element.metadata().displayLabel()), state);
        } catch (RuntimeException ignored) {
            // Visual diagnostics are best effort and never prevent or replace a native Selenium call.
        }
    }

    private ObservedElement observedByThis(WebElement element) {
        if (!(element instanceof WebDriverDecorator.HasTarget<?> holder)) return null;
        Decorated<?> target = holder.getTarget();
        return target instanceof ObservedElement observed && target.getDecorator() == this ? observed : null;
    }

    private void validateOwnership(WebElement element) {
        if (!(element instanceof WrapsDriver wraps)) return;
        WebDriver elementDriver = root(wraps.getWrappedDriver());
        WebDriver lensDriver = root(attachedDriver);
        if (elementDriver == lensDriver) return;
        if (elementDriver instanceof RemoteWebDriver elementSession && lensDriver instanceof RemoteWebDriver lensSession
                && elementSession.getSessionId() != null && lensSession.getSessionId() != null
                && !elementSession.getSessionId().equals(lensSession.getSessionId())) {
            throw new IllegalArgumentException("WebElement belongs to a different WebDriver session");
        }
    }

    private static WebDriver root(WebDriver driver) {
        WebDriver current = driver;
        Set<WebDriver> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current instanceof WrapsDriver wraps && seen.add(current)) {
            WebDriver next = wraps.getWrappedDriver();
            if (next == null || next == current) break;
            current = next;
        }
        return current;
    }

    private final class ObservedElement extends DefaultDecorated<WebElement> {
        private final ElementMetadata metadata;
        private ObservedElement(WebElement original, ElementMetadata metadata) {
            super(original, NativeSeleniumObserver.this);
            this.metadata = metadata == null ? ElementMetadata.unknown() : metadata;
        }
    }

    private final class ObservedShadowRoot implements SearchContext {
        private final SearchContext original;
        private final ElementMetadata parent;

        private ObservedShadowRoot(SearchContext original, ElementMetadata parent) {
            this.original = original;
            this.parent = parent;
        }

        @Override
        public WebElement findElement(By by) {
            return observeShadowResult(original.findElement(by), by);
        }

        @Override
        public List<WebElement> findElements(By by) {
            return original.findElements(by).stream().map(element -> observeShadowResult(element, by)).toList();
        }

        private WebElement observeShadowResult(WebElement element, By by) {
            String scope = parent.displayLabel() + " shadow root";
            return createProxy(new ObservedElement(element,
                    new ElementMetadata(by == null ? "" : by.toString(), "", scope)), WebElement.class);
        }
    }

    private enum Kind { ACTION, TECHNICAL, NONE }

    private record ElementMetadata(String selector, String label, String scope) {
        private static ElementMetadata unknown() { return new ElementMetadata("", "", "element"); }
        private ElementMetadata withLabel(String value) { return new ElementMetadata(selector, value, scope); }
        private String displayLabel() {
            if (!label.isBlank()) return label;
            if (!selector.isBlank()) return selector;
            return scope.isBlank() ? "element" : scope;
        }
        private TargetDescriptor target() {
            return new TargetDescriptor(selector.isBlank() ? null : selector,
                    label.isBlank() ? null : label, null, null,
                    scope.isBlank() ? java.util.Map.of() : java.util.Map.of("scope", scope));
        }
    }

    private record ElementTarget(WebElement original, ElementMetadata metadata) {}

    private record Invocation(Kind kind,
                              String action,
                              String description,
                              TargetDescriptor target,
                              List<ElementTarget> targets,
                              ElementMetadata resultMetadata,
                              boolean observed) {
        private boolean requiresOverlaySuppression() {
            return observed && "selenium.click".equals(action) && !targets.isEmpty();
        }

        private static Invocation create(Decorated<?> decorated, Method method, Object[] args, boolean active) {
            String name = method.getName();
            Object original = decorated.getOriginal();
            ElementMetadata element = decorated instanceof NativeSeleniumObserver.ObservedElement observed
                    ? observed.metadata : null;
            Kind kind = classify(original, method);
            ElementMetadata result = resultMetadata(original, name, args, element);
            List<ElementTarget> targets = targets(original, name, args, element, decorated.getDecorator());
            TargetDescriptor target = element == null ? TargetDescriptor.none() : element.target();
            if (element == null && !targets.isEmpty()) target = targets.get(0).metadata().target();
            String action = action(original, name);
            String description = description(action, element);
            return new Invocation(kind, action, description, target, targets, result, active && kind != Kind.NONE);
        }

        private static Kind classify(Object original, Method method) {
            String name = method.getName();
            if (original instanceof WebElement) {
                if (Set.of("click", "clear", "sendKeys", "submit").contains(name)) return Kind.ACTION;
                if (name.startsWith("findElement") || Set.of("getText", "getAttribute", "getDomAttribute",
                        "getDomProperty", "getRect", "getCssValue", "getLocation", "getSize", "getTagName",
                        "isDisplayed", "isEnabled", "isSelected", "getShadowRoot").contains(name)) return Kind.TECHNICAL;
            }
            if (original instanceof WebDriver) {
                if (Set.of("get", "close", "quit").contains(name)
                        || method.getDeclaringClass() == JavascriptExecutor.class
                        || method.getDeclaringClass() == Interactive.class) return Kind.ACTION;
                if (name.startsWith("findElement")) return Kind.TECHNICAL;
            }
            if (original instanceof WebDriver.Navigation
                    || (original instanceof WebDriver.TargetLocator && !"alert".equals(name))) {
                return "activeElement".equals(name) ? Kind.TECHNICAL : Kind.ACTION;
            }
            return Kind.NONE;
        }

        private static String action(Object original, String name) {
            if (original instanceof JavascriptExecutor) return "selenium." + name;
            if (original instanceof Interactive) return "selenium." + name;
            if (original instanceof WebDriver.Navigation) return "selenium.navigate." + name;
            if (original instanceof WebDriver.TargetLocator) return "selenium.switchTo." + name;
            return "selenium." + name;
        }

        private static String description(String action, ElementMetadata metadata) {
            return metadata == null ? action : action + ": " + metadata.displayLabel();
        }

        private static ElementMetadata resultMetadata(Object original, String name, Object[] args,
                                                      ElementMetadata parent) {
            if ((original instanceof SearchContext) && ("findElement".equals(name) || "findElements".equals(name))
                    && args != null && args.length > 0 && args[0] instanceof By by) {
                String selector = by.toString();
                String scope = parent == null ? "driver" : parent.displayLabel();
                return new ElementMetadata(selector, "", scope);
            }
            if (original instanceof WebDriver.TargetLocator && "activeElement".equals(name)) {
                return new ElementMetadata("", "", "active element");
            }
            return ElementMetadata.unknown();
        }

        private static List<ElementTarget> targets(Object original, String name, Object[] args,
                                                   ElementMetadata element,
                                                   WebDriverDecorator<?> decorator) {
            if (original instanceof WebElement && element != null
                    && Set.of("click", "clear", "sendKeys", "submit").contains(name)) {
                return List.of(new ElementTarget((WebElement) original, element));
            }
            if (!(original instanceof JavascriptExecutor) || args == null || args.length < 2
                    || !(args[1] instanceof Object[] scriptArgs)) return List.of();
            List<ElementTarget> found = new ArrayList<>();
            Set<WebElement> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Object value : scriptArgs) {
                if (!(value instanceof WebDriverDecorator.HasTarget<?> holder)) continue;
                Decorated<?> target = holder.getTarget();
                if (!(target instanceof NativeSeleniumObserver.ObservedElement observed)
                        || target.getDecorator() != decorator || !seen.add(observed.getOriginal())) continue;
                found.add(new ElementTarget(observed.getOriginal(), observed.metadata));
            }
            return List.copyOf(found);
        }

    }
}
