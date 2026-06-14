package io.github.mmaciekk111.uitestlens.react;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * React-specific entry points layered on top of the Selenium facade.
 */
public final class ReactSupport {
    private ReactSupport() {}

    public static ReactSafeExecutor reactSafe(JsOverlayDebug overlay) {
        Objects.requireNonNull(overlay, "overlay must not be null");
        return new ReactSafeExecutor(overlay.getDriver(), overlaySupport(overlay));
    }

    public static ReactOverlaySupport overlaySupport(JsOverlayDebug overlay) {
        Objects.requireNonNull(overlay, "overlay must not be null");
        return new ReactOverlaySupport() {
            @Override
            public OverlayConfig getConfig() {
                return overlay.getConfig();
            }

            @Override
            public void setStep(String stepDescription) {
                overlay.setStep(stepDescription);
            }

            @Override
            public WebElement highlightElement(WebElement element, String label) {
                return overlay.highlightElement(element, label);
            }
        };
    }

    public static void smartClick(JsOverlayDebug overlay, By locator, String label) {
        ReactSafeExecutor reactSafe = reactSafe(overlay);
        reactSafe.doWithRetry(
                locator,
                "SMART_CLICK_REACT_SAFE: " + label,
                element -> {
                    overlay.smartClickWithOverlayHandler(element, label);
                    return null;
                }
        );
    }

    public static WebElement findBySelectorContainingText(JsOverlayDebug overlay,
                                                          String selector,
                                                          String text,
                                                          boolean visibleOnly,
                                                          String description) {
        By by = toBy(selector);
        String needle = text == null ? "" : text.trim();

        return findFirst(overlay, by, el -> {
            if (visibleOnly && !el.isDisplayed()) {
                return false;
            }
            String tc = safeTextContent(overlay.getDriver(), el);
            return tc != null && tc.contains(needle);
        }, description + " | selector=" + selector + " | text=" + needle);
    }

    public static WebElement findFirst(JsOverlayDebug overlay,
                                       By listLocator,
                                       Predicate<WebElement> predicate,
                                       String description) {
        ReactSafeExecutor reactSafe = reactSafe(overlay);
        return reactSafe.doWithRetry(listLocator, "FIND_FIRST: " + description, ignored -> {
            List<WebElement> list = overlay.getDriver().findElements(listLocator);

            for (WebElement el : list) {
                try {
                    if (predicate.test(el)) {
                        if (overlay.getConfig().isEnabled() && overlay.getConfig().isShowHudPanel()) {
                            overlay.highlightElement(el, "FOUND");
                        }
                        return el;
                    }
                } catch (StaleElementReferenceException ignoredStale) {
                    // React re-rendered while evaluating this candidate. Continue within this attempt.
                }
            }

            throw new NoSuchElementException("No match in list for: " + description + " | locator=" + listLocator);
        });
    }

    public static List<WebElement> findChildren(JsOverlayDebug overlay,
                                                By parentListLocator,
                                                Predicate<WebElement> parentPredicate,
                                                By childLocator,
                                                String description) {
        ReactSafeExecutor reactSafe = reactSafe(overlay);
        return reactSafe.doWithRetry(parentListLocator, "FIND_CHILDREN: " + description, ignored -> {
            WebElement parent = findFirst(overlay, parentListLocator, parentPredicate, description + " (parent)");
            return parent.findElements(childLocator);
        });
    }

    public static WebElement findChildByText(JsOverlayDebug overlay,
                                             By parentListLocator,
                                             Predicate<WebElement> parentPredicate,
                                             By childLocator,
                                             Predicate<WebElement> childPredicate,
                                             String description) {
        ReactSafeExecutor reactSafe = reactSafe(overlay);
        return reactSafe.doWithRetry(parentListLocator, description, ignored -> {
            WebElement parent = findFirst(overlay, parentListLocator, parentPredicate, description + " (parent)");

            List<WebElement> children = parent.findElements(childLocator);
            for (WebElement child : children) {
                if (childPredicate.test(child)) {
                    overlay.highlightElement(child, "FOUND");
                    return child;
                }
            }

            throw new NoSuchElementException("Child not found: " + description);
        });
    }

    public static WebElement findChildByTextThenFind(JsOverlayDebug overlay,
                                                     By parentListLocator,
                                                     Predicate<WebElement> parentPredicate,
                                                     By childLocator,
                                                     Predicate<WebElement> childPredicate,
                                                     By innerLocator,
                                                     String description) {
        ReactSafeExecutor reactSafe = reactSafe(overlay);
        return reactSafe.doWithRetry(parentListLocator, description, ignored -> {
            WebElement parent = findFirst(overlay, parentListLocator, parentPredicate, description + " (parent)");

            List<WebElement> children = parent.findElements(childLocator);
            for (WebElement child : children) {
                try {
                    if (childPredicate.test(child)) {
                        WebElement inner = child.findElement(innerLocator);
                        overlay.highlightElement(inner, "FOUND");
                        return inner;
                    }
                } catch (NoSuchElementException ignoredMissingInner) {
                    // Keep scanning siblings.
                }
            }

            throw new NoSuchElementException("Inner element not found: " + description);
        });
    }

    private static By toBy(String selectorRaw) {
        String s = selectorRaw == null ? "" : selectorRaw.trim();
        if (s.toLowerCase().startsWith("xpath=") || s.toLowerCase().startsWith("xpath:")) {
            s = s.replaceFirst("(?i)^xpath\\s*[:=]\\s*", "");
            return By.xpath(s);
        }
        if (s.toLowerCase().startsWith("css=") || s.toLowerCase().startsWith("css:")) {
            s = s.replaceFirst("(?i)^css\\s*[:=]\\s*", "");
            return By.cssSelector(s);
        }
        if (s.startsWith("/") || s.startsWith("./") || s.startsWith("(")) {
            return By.xpath(s);
        }
        return By.cssSelector(s);
    }

    private static String safeTextContent(WebDriver driver, WebElement el) {
        try {
            Object v = ((JavascriptExecutor) driver).executeScript(
                    "return (arguments[0] && (arguments[0].textContent || arguments[0].innerText)) || '';",
                    el
            );
            return v == null ? "" : String.valueOf(v).trim();
        } catch (StaleElementReferenceException e) {
            throw e;
        }
    }
}
