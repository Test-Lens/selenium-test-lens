package io.github.testlens;

import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
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
import java.util.Map;

/** Internal Selenium decorator. It observes calls but never changes their execution policy. */
final class NativeSeleniumObserver extends WebDriverDecorator<WebDriver> {
    private final JsOverlayDebug lens;
    private final WebDriver attachedDriver;
    private final ThreadLocal<Invocation> invocation = new ThreadLocal<>();
    private final ContextState contextState = new ContextState();
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

    void beginSession() {
        contextState.beginSession();
    }

    WebElement observe(WebElement element, String label) {
        if (element == null) throw new IllegalArgumentException("element must not be null");
        ObservedElement existing = observedByThis(element);
        String effectiveLabel = lens.redactObservationLabel(label == null ? "" : label);
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
        Invocation current = Invocation.create(target, method, args, lens, contextState.snapshot(), System.nanoTime());
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
            contextState.afterSuccess(target.getOriginal(), method.getName(), args, current);
            if (current == null || !current.observed()) return;
            if (current.kind() == Kind.ACTION) {
                lens.emitNativeOperation(current.action(), current.currentDescription(), UiTestLensStatus.PASSED,
                        UiTestLensLogLevel.INFO, current.currentTarget(), null);
                current.targets().forEach(element -> highlight(element, HighlightState.SUCCESS));
            } else if (current.kind() == Kind.TECHNICAL) {
                lens.emitNativeTechnical(current.action(), current.description(), current.target(), null,
                        current.locatorObservation(result, null));
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
                    lens.emitNativeOperation(current.action(), current.currentDescription(), UiTestLensStatus.FAILED,
                            UiTestLensLogLevel.ERROR, current.currentTarget(), original);
                    current.targets().forEach(element -> highlight(element, HighlightState.FAILURE));
                } else if (current.kind() == Kind.TECHNICAL) {
                    // Polling/read failures remain technical diagnostics, never functional failures.
                    lens.emitNativeTechnical(current.action(), current.description(), current.target(), original,
                            current.locatorObservation(null, original));
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
            long started = System.nanoTime();
            LocatorObservationMetadata.Locator locator = safeLocator(by);
            LocatorObservationMetadata.Context context = shadowContext();
            try {
                WebElement result = original.findElement(by);
                emitShadowObservation(locator, context, "FIND_ONE", "RESOLVED", null, started, null);
                return observeShadowResult(result, locator, context);
            } catch (RuntimeException failure) {
                emitShadowObservation(locator, context, "FIND_ONE",
                        failure instanceof NoSuchElementException ? "NOT_FOUND" : "ERROR", null, started, failure);
                throw failure;
            }
        }

        @Override
        public List<WebElement> findElements(By by) {
            long started = System.nanoTime();
            LocatorObservationMetadata.Locator locator = safeLocator(by);
            LocatorObservationMetadata.Context context = shadowContext();
            try {
                List<WebElement> result = original.findElements(by);
                emitShadowObservation(locator, context, "FIND_MANY", "RESOLVED", result.size(), started, null);
                return result.stream().map(element -> observeShadowResult(element, locator, context)).toList();
            } catch (RuntimeException failure) {
                emitShadowObservation(locator, context, "FIND_MANY", "ERROR", null, started, failure);
                throw failure;
            }
        }

        private LocatorObservationMetadata.Locator safeLocator(By by) {
            return LocatorObservationMetadata.locator(by, "").redacted(lens::redactObservationLabel);
        }

        private LocatorObservationMetadata.Context shadowContext() {
            String scope = parent.displayLabel() + " shadow root";
            return parent.context().append(
                    new LocatorObservationMetadata.Segment("SHADOW_ROOT",
                            parent.locatorKnown() ? "KNOWN" : "PARTIAL",
                            parent.locatorKnown() ? parent.locator() : null,
                            parent.label(), null, ""));
        }

        private void emitShadowObservation(LocatorObservationMetadata.Locator locator,
                                           LocatorObservationMetadata.Context context, String intent,
                                           String outcome, Integer matchCount, long started, Throwable failure) {
            Map<String, String> observation = LocatorObservationMetadata.observation(locator, context, intent,
                    outcome, matchCount, Math.max(0L, System.nanoTime() - started));
            lens.emitNativeTechnical("selenium." + ("FIND_MANY".equals(intent) ? "findElements" : "findElement"),
                    "shadow root " + ("FIND_MANY".equals(intent) ? "findElements" : "findElement"),
                    new TargetDescriptor(locator.display(), null, null, null, Map.of("scope", "shadow root")),
                    failure, observation);
        }

        private WebElement observeShadowResult(WebElement element, LocatorObservationMetadata.Locator locator,
                                               LocatorObservationMetadata.Context context) {
            String scope = parent.displayLabel() + " shadow root";
            return createProxy(new ObservedElement(element,
                    new ElementMetadata(locator, "", scope, context)),
                    WebElement.class);
        }
    }

    private enum Kind { ACTION, TECHNICAL, NONE }

    private static final class ElementMetadata {
        private final LocatorObservationMetadata.Locator locator;
        private final String label;
        private final String scope;
        private final LocatorObservationMetadata.Context context;

        private ElementMetadata(LocatorObservationMetadata.Locator locator, String label, String scope,
                                LocatorObservationMetadata.Context context) {
            this.locator = locator;
            this.label = label == null ? "" : label;
            this.scope = scope == null ? "" : scope;
            this.context = context == null ? LocatorObservationMetadata.unknown() : context;
        }

        private static ElementMetadata unknown() {
            return new ElementMetadata(LocatorObservationMetadata.Locator.unknown(""), "", "element",
                    LocatorObservationMetadata.unknown());
        }
        private ElementMetadata withLabel(String value) {
            return new ElementMetadata(locator.withLabel(value), value, scope, context);
        }
        private LocatorObservationMetadata.Locator locator() { return locator; }
        private String label() { return label; }
        private LocatorObservationMetadata.Context context() { return context; }
        private boolean locatorKnown() { return locator != null && !locator.display().equals("Unknown locator"); }
        private String compactLocator() { return locatorKnown() ? LocatorObservationMetadata.compact(locator) : ""; }
        private String humanLabel() { return label; }
        private String labelProvenance() {
            return !label.isBlank() ? "EXPLICIT_USER" : "NONE";
        }
        private String displayLabel() {
            if (!humanLabel().isBlank()) return humanLabel();
            if (locatorKnown()) return compactLocator();
            return scope.isBlank() ? "element" : scope;
        }
        private TargetDescriptor target() {
            Map<String, String> metadata = new java.util.LinkedHashMap<>();
            if (!scope.isBlank()) metadata.put("scope", scope);
            metadata.put("labelProvenance", labelProvenance());
            return new TargetDescriptor(compactLocator().isBlank() ? null : compactLocator(),
                    humanLabel().isBlank() ? null : humanLabel(), null, null, metadata);
        }
    }

    private record ElementTarget(WebElement original, ElementMetadata metadata) {}

    private record Invocation(Kind kind,
                              String action,
                              String description,
                              TargetDescriptor target,
                              List<ElementTarget> targets,
                              ElementMetadata resultMetadata,
                              boolean observed,
                              LocatorObservationMetadata.Locator locator,
                              String usageIntent,
                              LocatorObservationMetadata.Context locatorContext,
                              long startedNanos,
                              ElementMetadata argumentElement) {
        private boolean requiresOverlaySuppression() {
            return observed && "selenium.click".equals(action) && !targets.isEmpty();
        }

        private static Invocation create(Decorated<?> decorated, Method method, Object[] args, JsOverlayDebug lens,
                                         LocatorObservationMetadata.Context driverContext, long startedNanos) {
            String name = method.getName();
            Object original = decorated.getOriginal();
            ElementMetadata element = decorated instanceof NativeSeleniumObserver.ObservedElement observed
                    ? observed.metadata : null;
            Kind kind = classify(original, method);
            By locatorBy = findBy(original, name, args);
            LocatorObservationMetadata.Locator locator = locatorBy == null ? null
                    : LocatorObservationMetadata.locator(locatorBy, "").redacted(lens::redactObservationLabel);
            ElementMetadata result = resultMetadata(original, name, element, driverContext, locator);
            List<ElementTarget> targets = targets(original, name, args, element, decorated.getDecorator());
            TargetDescriptor target = element == null ? TargetDescriptor.none() : element.target();
            if (element == null && !targets.isEmpty()) target = targets.get(0).metadata().target();
            String action = action(original, name);
            String description = description(action, element);
            String usageIntent = "findElements".equals(name) ? "FIND_MANY"
                    : "findElement".equals(name) ? "FIND_ONE" : "UNKNOWN";
            LocatorObservationMetadata.Context locatorContext = searchContext(original, element, driverContext);
            return new Invocation(kind, action, description, target, targets, result,
                    lens.observationActive() && kind != Kind.NONE, locator, usageIntent, locatorContext, startedNanos,
                    argumentElement(args, decorated.getDecorator()));
        }

        private ElementMetadata primaryMetadata() {
            return targets.isEmpty() ? null : targets.get(0).metadata();
        }

        private String currentDescription() {
            ElementMetadata metadata = primaryMetadata();
            return metadata == null ? description : description(action, metadata);
        }

        private TargetDescriptor currentTarget() {
            ElementMetadata metadata = primaryMetadata();
            return metadata == null ? target : metadata.target();
        }

        private Map<String, String> locatorObservation(Object result, Throwable failure) {
            if (locator == null) return Map.of();
            String outcome = failure == null ? "RESOLVED"
                    : failure instanceof NoSuchElementException ? "NOT_FOUND" : "ERROR";
            Integer count = "FIND_MANY".equals(usageIntent) && result instanceof List<?> values
                    ? values.size() : null;
            return LocatorObservationMetadata.observation(
                    locator, locatorContext, usageIntent,
                    outcome, count, Math.max(0L, System.nanoTime() - startedNanos));
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

        private static ElementMetadata resultMetadata(Object original, String name, ElementMetadata parent,
                                                      LocatorObservationMetadata.Context driverContext,
                                                      LocatorObservationMetadata.Locator locator) {
            if ((original instanceof SearchContext) && ("findElement".equals(name) || "findElements".equals(name))
                    && locator != null) {
                String scope = parent == null ? "driver" : parent.displayLabel();
                LocatorObservationMetadata.Context context = searchContext(original, parent, driverContext);
                return new ElementMetadata(locator, "", scope, context);
            }
            if (original instanceof WebDriver.TargetLocator && "activeElement".equals(name)) {
                return new ElementMetadata(LocatorObservationMetadata.Locator.unknown(""), "", "active element",
                        driverContext);
            }
            return ElementMetadata.unknown();
        }

        private static By findBy(Object original, String name, Object[] args) {
            return original instanceof SearchContext
                    && ("findElement".equals(name) || "findElements".equals(name))
                    && args != null && args.length > 0 && args[0] instanceof By by ? by : null;
        }

        private static LocatorObservationMetadata.Context searchContext(Object original, ElementMetadata parent,
                                                                         LocatorObservationMetadata.Context driver) {
            if (!(original instanceof WebElement) || parent == null) return driver;
            return parent.context().append(new LocatorObservationMetadata.Segment("ELEMENT",
                    parent.locatorKnown() ? "KNOWN" : "PARTIAL",
                    parent.locatorKnown() ? parent.locator() : null, parent.label(), null, ""));
        }

        private static ElementMetadata argumentElement(Object[] args, WebDriverDecorator<?> decorator) {
            if (args == null || args.length == 0 || !(args[0] instanceof WebDriverDecorator.HasTarget<?> holder)) {
                return null;
            }
            Decorated<?> target = holder.getTarget();
            if (target instanceof NativeSeleniumObserver.ObservedElement observed
                    && target.getDecorator() == decorator) return observed.metadata;
            return null;
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

    private final class ContextState {
        private LocatorObservationMetadata.Context context = LocatorObservationMetadata.root();
        private final List<LocatorObservationMetadata.Segment> frames = new ArrayList<>();

        synchronized void beginSession() {
            context = LocatorObservationMetadata.unknown();
            frames.clear();
        }

        synchronized LocatorObservationMetadata.Context snapshot() { return context; }

        synchronized void afterSuccess(Object original, String name, Object[] args, Invocation invocation) {
            if ((original instanceof WebDriver && "get".equals(name))
                    || original instanceof WebDriver.Navigation) {
                frames.clear();
                context = rootForCurrentWindow(context);
                return;
            }
            if (!(original instanceof WebDriver.TargetLocator)) return;
            if ("defaultContent".equals(name)) {
                frames.clear();
                context = rootForCurrentWindow(context);
                return;
            }
            if ("parentFrame".equals(name)) {
                if (!frames.isEmpty()) frames.remove(frames.size() - 1);
                else context = partial(context);
                rebuild();
                return;
            }
            if ("window".equals(name)) {
                frames.clear();
                String handle = args != null && args.length > 0 && args[0] != null
                        ? lens.redactObservationLabel(String.valueOf(args[0])) : "";
                context = LocatorObservationMetadata.root().append(new LocatorObservationMetadata.Segment(
                        "WINDOW", handle.isBlank() ? "PARTIAL" : "KNOWN", null, "",
                        handle.isBlank() ? null : "SESSION_LOCAL_HANDLE", handle));
                return;
            }
            if ("newWindow".equals(name)) {
                frames.clear();
                context = LocatorObservationMetadata.root().append(new LocatorObservationMetadata.Segment(
                        "WINDOW", "PARTIAL", null, "", null, ""));
                return;
            }
            if (!"frame".equals(name)) return;
            LocatorObservationMetadata.Segment frame = frameSegment(args, invocation);
            frames.add(frame);
            context = context.append(frame);
        }

        private LocatorObservationMetadata.Segment frameSegment(Object[] args, Invocation invocation) {
            Object value = args == null || args.length == 0 ? null : args[0];
            if (value instanceof Integer index) return new LocatorObservationMetadata.Segment(
                    "FRAME", "KNOWN", null, "", "INDEX", String.valueOf(index));
            if (value instanceof String name) return new LocatorObservationMetadata.Segment(
                    "FRAME", "KNOWN", null, "", "NAME_OR_ID", lens.redactObservationLabel(name));
            ElementMetadata element = invocation == null ? null : invocation.argumentElement();
            return new LocatorObservationMetadata.Segment("FRAME",
                    element != null && element.locatorKnown() ? "KNOWN" : "PARTIAL",
                    element != null && element.locatorKnown() ? element.locator() : null,
                    element == null ? "" : element.label(), null, "");
        }

        private void rebuild() {
            LocatorObservationMetadata.Context base = rootForCurrentWindow(context);
            context = base;
            for (LocatorObservationMetadata.Segment frame : frames) context = context.append(frame);
        }

        private static LocatorObservationMetadata.Context rootForCurrentWindow(LocatorObservationMetadata.Context current) {
            List<LocatorObservationMetadata.Segment> segments = current.segments();
            LocatorObservationMetadata.Context base = LocatorObservationMetadata.root();
            for (LocatorObservationMetadata.Segment segment : segments) {
                if ("WINDOW".equals(segment.kind())) return base.append(segment);
            }
            return base;
        }

        private static LocatorObservationMetadata.Context partial(LocatorObservationMetadata.Context current) {
            return new LocatorObservationMetadata.Context("PARTIAL", current.segments());
        }
    }
}
