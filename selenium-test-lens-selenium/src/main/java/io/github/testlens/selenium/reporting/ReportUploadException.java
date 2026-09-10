package io.github.testlens.selenium.reporting;

/** Thrown by {@link ReportUploadResult#requireSuccess()} for a non-successful upload. */
public final class ReportUploadException extends RuntimeException {
    private final ReportUploadResult result;

    ReportUploadException(ReportUploadResult result) {
        super(result.failureMessage(), result.exception());
        this.result = result;
    }

    /** Returns the immutable failed or skipped upload result. */
    public ReportUploadResult result() { return result; }
}
