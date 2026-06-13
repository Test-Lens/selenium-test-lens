package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PageWaits {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final Duration defaultTimeout;

    public PageWaits(WebDriver driver, OverlayConfig config) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.defaultTimeout = Duration.ofSeconds(10);
    }

    public PageWaits(WebDriver driver, OverlayConfig config, Duration defaultTimeout) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.defaultTimeout = defaultTimeout != null ? defaultTimeout : Duration.ofSeconds(10);
    }

    // === Helpers do logowania czasu ===

    private void rememberLastWaitMessage(String message) {
        try {
            js.executeScript(
                    UiTestLensRuntimeNames.ensureNamespaceScript() +
                            "window.__uiTestLens.state.wait.lastMessage = arguments[0];" +
                            "window.__seleniumLastWaitMessage = window.__uiTestLens.state.wait.lastMessage;",
                    message
            );
        } catch (Exception ignored) {
            // brak okna / błąd JS – trudno, po prostu bez HUD info
        }
    }

    private long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }

    // === klasyczne waity ===

    /**
     * Czeka aż document.readyState == 'complete' + zapisuje info dla HUD-a.
     */
    public void waitForDocumentReady() {
        waitForDocumentReady(defaultTimeout);
    }

    public void waitForDocumentReady(Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;

        long start = nowMs();
        WebDriverWait wait = new WebDriverWait(driver, effectiveTimeout);
        wait.until((ExpectedCondition<Boolean>) d -> {
            Object result = js.executeScript("return document.readyState");
            return "complete".equals(result);
        });
        long elapsed = nowMs() - start;

        rememberLastWaitMessage(String.format(
                "[WAIT] document.readyState == 'complete' in %d ms (timeout %d ms)",
                elapsed,
                effectiveTimeout.toMillis()
        ));
    }

    /**
     * Czeka aż document.readyState będzie 'interactive' lub 'complete'.
     */
    public void waitForInteractiveOrComplete() {
        waitForInteractiveOrComplete(defaultTimeout);
    }

    public void waitForInteractiveOrComplete(Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;

        long start = nowMs();
        WebDriverWait wait = new WebDriverWait(driver, effectiveTimeout);
        wait.until((ExpectedCondition<Boolean>) d -> {
            Object result = js.executeScript("return document.readyState");
            if (result == null) {
                return false;
            }
            String state = result.toString();
            return "interactive".equals(state) || "complete".equals(state);
        });
        long elapsed = nowMs() - start;

        rememberLastWaitMessage(String.format(
                "[WAIT] document.readyState in {interactive, complete} in %d ms (timeout %d ms)",
                elapsed,
                effectiveTimeout.toMillis()
        ));
    }

    /**
     * Prosta wersja "network idle":
     * - trackuje aktywne XHR/fetch w oknie,
     * - czeka, aż przez określony czas nie pojawi się żaden aktywny request.
     */
    public void waitForNetworkIdle(Duration idleDuration, Duration timeout) {
        if (idleDuration == null) {
            idleDuration = Duration.ofMillis(500);
        }
        if (timeout == null) {
            timeout = defaultTimeout;
        }

        injectNetworkTrackerIfNeeded();

        long idleMillis = idleDuration.toMillis();
        long timeoutMillis = timeout.toMillis();

        long start = nowMs();
        long lastActive = nowMs();
        boolean idleAchieved = false;

        while (true) {
            Long active = null;
            try {
                active = ((Number) js.executeScript(
                        UiTestLensRuntimeNames.ensureNamespaceScript() +
                                "return window.__uiTestLens.state.network.activeRequests || window.__seleniumActiveRequests || 0;"
                )).longValue();
            } catch (Exception ignored) {
            }

            long now = nowMs();

            if (active != null && active == 0L) {
                if (now - lastActive >= idleMillis) {
                    idleAchieved = true;
                    break;
                }
            } else {
                lastActive = now;
            }

            if (now - start > timeoutMillis) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        long elapsed = nowMs() - start;
        if (idleAchieved) {
            rememberLastWaitMessage(String.format(
                    "[WAIT] network idle (%d ms idle window) in %d ms (timeout %d ms)",
                    idleMillis,
                    elapsed,
                    timeoutMillis
            ));
        } else {
            rememberLastWaitMessage(String.format(
                    "[WAIT] network NOT idle within timeout %d ms (idle window %d ms, elapsed %d ms)",
                    timeoutMillis,
                    idleMillis,
                    elapsed
            ));
        }
    }

    public void waitForNetworkIdle() {
        waitForNetworkIdle(Duration.ofMillis(500), defaultTimeout);
    }

    /**
     * Wstrzykuje prosty tracker XHR/fetch do strony (jeśli jeszcze nie istnieje).
     */
    private void injectNetworkTrackerIfNeeded() {
        js.executeScript(
                UiTestLensRuntimeNames.ensureNamespaceScript() +
                        "if (window.__uiTestLens.state.network.trackerInstalled || window.__seleniumNetworkTrackerInstalled) {" +
                        "  window.__uiTestLens.state.network.trackerInstalled = true;" +
                        "  window.__seleniumNetworkTrackerInstalled = true;" +
                        "  window.__uiTestLens.state.network.activeRequests = window.__uiTestLens.state.network.activeRequests || window.__seleniumActiveRequests || 0;" +
                        "  window.__seleniumActiveRequests = window.__uiTestLens.state.network.activeRequests;" +
                        "  return;" +
                        "}" +
                        "window.__uiTestLens.state.network.trackerInstalled = true;" +
                        "window.__seleniumNetworkTrackerInstalled = true;" +
                        "window.__uiTestLens.state.network.activeRequests = 0;" +
                        "window.__seleniumActiveRequests = 0;" +

                        // hook na XHR
                        "(function() {" +
                        "  var origOpen = XMLHttpRequest.prototype.open;" +
                        "  var origSend = XMLHttpRequest.prototype.send;" +
                        "  XMLHttpRequest.prototype.open = function() {" +
                        "    origOpen.apply(this, arguments);" +
                        "  };" +
                        "  XMLHttpRequest.prototype.send = function() {" +
                        "    window.__uiTestLens.state.network.activeRequests++;" +
                        "    window.__seleniumActiveRequests = window.__uiTestLens.state.network.activeRequests;" +
                        "    this.addEventListener('loadend', function() {" +
                        "      window.__uiTestLens.state.network.activeRequests--;" +
                        "      window.__seleniumActiveRequests = window.__uiTestLens.state.network.activeRequests;" +
                        "    });" +
                        "    origSend.apply(this, arguments);" +
                        "  };" +
                        "})();" +

                        // hook na fetch
                        "(function() {" +
                        "  if (!window.fetch) { return; }" +
                        "  var origFetch = window.fetch;" +
                        "  window.fetch = function() {" +
                        "    window.__uiTestLens.state.network.activeRequests++;" +
                        "    window.__seleniumActiveRequests = window.__uiTestLens.state.network.activeRequests;" +
                        "    return origFetch.apply(this, arguments)" +
                        "      .finally(function() {" +
                        "        window.__uiTestLens.state.network.activeRequests--;" +
                        "        window.__seleniumActiveRequests = window.__uiTestLens.state.network.activeRequests;" +
                        "      });" +
                        "  };" +
                        "})();"
        );
    }

    // === REACT / SPA WAITY ===

    /**
     * 1) Czekaj aż "root" Reacta będzie zamontowany:
     *    - element wskazany locatorem istnieje,
     *    - ma co najmniej 1 dziecko (pierwszy render / hydracja zakończona).
     */
    public WebElement waitForReactRootMounted(By rootLocator) {
        return waitForReactRootMounted(rootLocator, defaultTimeout);
    }

    public WebElement waitForReactRootMounted(By rootLocator, Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;
        long start = nowMs();

        WebDriverWait wait = new WebDriverWait(driver, effectiveTimeout);
        WebElement root = wait.until(d -> {
            WebElement r = d.findElement(rootLocator);
            if (r == null) {
                return null;
            }
            Object hasChildren = js.executeScript(
                    "return (arguments[0] && arguments[0].children && arguments[0].children.length > 0);",
                    r
            );
            return Boolean.TRUE.equals(hasChildren) ? r : null;
        });

        long elapsed = nowMs() - start;
        rememberLastWaitMessage(String.format(
                "[WAIT] React root '%s' mounted (has children) in %d ms (timeout %d ms)",
                rootLocator,
                elapsed,
                effectiveTimeout.toMillis()
        ));
        return root;
    }

    /**
     * 2) Czekaj aż DOM pod danym rootem będzie "stabilny" (MutationObserver).
     */
    public void waitForSpaDomStableUnder(By rootLocator,
                                         Duration idleDuration,
                                         Duration timeout) {
        if (idleDuration == null) {
            idleDuration = Duration.ofMillis(300);
        }
        if (timeout == null) {
            timeout = defaultTimeout;
        }

        long start = nowMs();
        long idleMs = idleDuration.toMillis();

        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.until(d -> {
            WebElement root;
            try {
                root = d.findElement(rootLocator);
            } catch (NoSuchElementException e) {
                return false;
            }
            if (root == null) return false;

            Long lastMut = (Long) js.executeScript(
                    "var root = arguments[0];" +
                            "if (!root) return -1;" +
                            "if (!root.__seleniumDomStableInit) {" +
                            "  root.__seleniumDomStableInit = true;" +
                            "  root.__seleniumLastMutation = Date.now();" +
                            "  var obs = new MutationObserver(function(mutations) {" +
                            "    root.__seleniumLastMutation = Date.now();" +
                            "  });" +
                            "  obs.observe(root, {" +
                            "    childList: true," +
                            "    subtree: true," +
                            "    attributes: true," +
                            "    characterData: true" +
                            "  });" +
                            "}" +
                            "return root.__seleniumLastMutation || Date.now();",
                    root
            );

            long now = System.currentTimeMillis();
            if (lastMut == null || lastMut <= 0L) {
                return false;
            }
            long diff = now - lastMut;
            return diff >= idleMs;
        });

        long elapsed = nowMs() - start;
        rememberLastWaitMessage(String.format(
                "[WAIT] SPA DOM stable under '%s' (no mutations for %d ms) in %d ms (timeout %d ms)",
                rootLocator,
                idleMs,
                elapsed,
                timeout.toMillis()
        ));
    }

    public void waitForSpaDomStableUnder(By rootLocator) {
        waitForSpaDomStableUnder(rootLocator, Duration.ofMillis(300), defaultTimeout);
    }

    /**
     * 3) Czekaj aż "komponent" będzie widoczny (root + stable + visibility).
     */
    public WebElement waitForReactComponentVisible(By rootLocator,
                                                   By componentLocator) {
        return waitForReactComponentVisible(rootLocator, componentLocator, defaultTimeout);
    }

    public WebElement waitForReactComponentVisible(By rootLocator,
                                                   By componentLocator,
                                                   Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;
        long start = nowMs();

        // 1) root mounted
        waitForReactRootMounted(rootLocator, effectiveTimeout);

        // 2) DOM stable pod rootem
        waitForSpaDomStableUnder(rootLocator, Duration.ofMillis(300), effectiveTimeout);

        // 3) komponent widoczny
        WebDriverWait wait = new WebDriverWait(driver, effectiveTimeout);
        WebElement component = wait.until(
                ExpectedConditions.visibilityOfElementLocated(componentLocator)
        );

        long elapsed = nowMs() - start;
        rememberLastWaitMessage(String.format(
                "[WAIT] React component '%s' visible under '%s' in %d ms (timeout %d ms)",
                componentLocator,
                rootLocator,
                elapsed,
                effectiveTimeout.toMillis()
        ));

        return component;
    }

    /**
     * 4) Combo: readyState + network idle + React root + stable DOM.
     */
    public void waitForReactAndNetworkIdle(By rootLocator) {
        waitForReactAndNetworkIdle(rootLocator, defaultTimeout);
    }

    public void waitForReactAndNetworkIdle(By rootLocator, Duration timeout) {
        Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;
        long start = nowMs();

        // 1) klasyczny page ready (z własnym logiem)
        waitForDocumentReady(effectiveTimeout);

        // 2) network idle (z własnym logiem)
        waitForNetworkIdle(Duration.ofMillis(500), effectiveTimeout);

        // 3) React root mounted (z własnym logiem)
        waitForReactRootMounted(rootLocator, effectiveTimeout);

        // 4) DOM stable pod rootem (z własnym logiem)
        waitForSpaDomStableUnder(rootLocator, Duration.ofMillis(300), effectiveTimeout);

        long elapsed = nowMs() - start;
        rememberLastWaitMessage(String.format(
                "[WAIT] React+network idle under '%s' (ready+network+root+stable DOM) in %d ms (timeout %d ms)",
                rootLocator,
                elapsed,
                effectiveTimeout.toMillis()
        ));
    }
}
