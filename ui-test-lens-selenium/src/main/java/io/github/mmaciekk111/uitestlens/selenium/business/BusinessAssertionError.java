package io.github.mmaciekk111.uitestlens.selenium.business;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public final class BusinessAssertionError extends AssertionError {
    private final String subject;
    private final List<BusinessAssertionResult> results;

    public BusinessAssertionError(String subject, List<BusinessAssertionResult> results, BusinessAssertionOptions options) {
        super(format(subject, results, options));
        this.subject = subject == null ? "" : subject;
        this.results = List.copyOf(results == null ? List.of() : results);
    }

    public String subject() {
        return subject;
    }

    public List<BusinessAssertionResult> results() {
        return results;
    }

    static String format(String subject, List<BusinessAssertionResult> results, BusinessAssertionOptions options) {
        BusinessAssertionOptions effectiveOptions = options == null ? BusinessAssertionOptions.defaults() : options;
        StringBuilder sb = new StringBuilder();
        sb.append("Business assertions failed for: ").append(subject == null ? "" : subject).append("\n\n");
        sb.append("Failed checks:\n");
        int index = 1;
        for (BusinessAssertionResult result : results == null ? List.<BusinessAssertionResult>of() : results) {
            if (result.status() != BusinessAssertionStatus.FAILED) {
                continue;
            }
            sb.append(index++).append(". ").append(result.description()).append("\n");
            BusinessAssertionFailure failure = result.failure();
            String message = failure == null ? result.message() : failure.message();
            sb.append("   ").append(BusinessAssertionFailure.preview(message, effectiveOptions.messagePreviewLimit())).append("\n");
            if (failure != null && !failure.assertionSummary().isBlank() && !failure.assertionSummary().equals(message)) {
                sb.append("   Assertion: ")
                        .append(BusinessAssertionFailure.preview(failure.assertionSummary(), effectiveOptions.messagePreviewLimit()))
                        .append("\n");
            }
            if (effectiveOptions.includeStackTrace() && failure != null && failure.cause() != null) {
                sb.append("   Stack trace: ")
                        .append(BusinessAssertionFailure.preview(stackTrace(failure.cause()), effectiveOptions.messagePreviewLimit()))
                        .append("\n");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
