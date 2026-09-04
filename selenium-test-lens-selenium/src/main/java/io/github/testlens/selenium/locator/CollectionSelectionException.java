package io.github.testlens.selenium.locator;

import org.openqa.selenium.NoSuchElementException;

/** Internal signal retaining collection selection diagnostics across WebDriverWait. */
final class CollectionSelectionException extends NoSuchElementException {
    CollectionSelectionException(String requested, int actualCount) {
        super("requestedIndex=" + requested + " | actualCount=" + actualCount);
    }
}
