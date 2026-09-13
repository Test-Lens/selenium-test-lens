package io.github.testlens.selenium.evidence;

final class VisualRedactionException extends RuntimeException {
    VisualRedactionException(String message) { super(message); }
    VisualRedactionException(String message, Throwable cause) { super(message, cause); }
}
