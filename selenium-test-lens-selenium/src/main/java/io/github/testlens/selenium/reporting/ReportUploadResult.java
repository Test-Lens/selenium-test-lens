package io.github.testlens.selenium.reporting;

import java.time.Duration;

/** Immutable outcome of one synchronous report upload operation. */
public final class ReportUploadResult {
    private final ReportUploadStatus status;
    private final ReportArtifactKind artifactKind;
    private final String endpoint;
    private final Integer httpStatus;
    private final int attempts;
    private final Duration elapsed;
    private final long payloadSize;
    private final String sha256;
    private final String serverReportId;
    private final String responsePreview;
    private final ReportUploadFailureCategory failureCategory;
    private final Throwable exception;
    private final String message;

    ReportUploadResult(ReportUploadStatus status, ReportArtifactKind artifactKind, String endpoint,
                       Integer httpStatus, int attempts, Duration elapsed, long payloadSize, String sha256,
                       String serverReportId, String responsePreview, ReportUploadFailureCategory failureCategory,
                       Throwable exception, String message) {
        this.status = status;
        this.artifactKind = artifactKind;
        this.endpoint = endpoint;
        this.httpStatus = httpStatus;
        this.attempts = attempts;
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
        this.payloadSize = Math.max(0, payloadSize);
        this.sha256 = sha256 == null ? "" : sha256;
        this.serverReportId = serverReportId;
        this.responsePreview = responsePreview == null ? "" : responsePreview;
        this.failureCategory = failureCategory == null ? ReportUploadFailureCategory.NONE : failureCategory;
        this.exception = exception;
        this.message = message == null ? "" : message;
    }

    /** Returns the transport outcome. */
    public ReportUploadStatus status() { return status; }
    /** Returns the selected payload kind, or {@code null} when no artifact was selected. */
    public ReportArtifactKind artifactKind() { return artifactKind; }
    /** Returns a safe endpoint description without query, userinfo, or fragment. */
    public String endpoint() { return endpoint; }
    /** Returns the HTTP response status, or {@code null} when no response arrived. */
    public Integer httpStatus() { return httpStatus; }
    /** Returns the number of HTTP attempts actually made. */
    public int attempts() { return attempts; }
    /** Returns monotonic elapsed time for preparation and transport. */
    public Duration elapsed() { return elapsed; }
    /** Returns the completed ZIP payload size in bytes. */
    public long payloadSize() { return payloadSize; }
    /** Returns the lowercase SHA-256 of the completed ZIP payload, when computed. */
    public String sha256() { return sha256; }
    /** Returns the bounded, redacted receiver identifier, if supplied. */
    public String serverReportId() { return serverReportId; }
    /** Returns the bounded, redacted UTF-8 response preview. */
    public String responsePreview() { return responsePreview; }
    /** Returns a stable reason category for non-success outcomes. */
    public ReportUploadFailureCategory failureCategory() { return failureCategory; }
    /** Returns the original transport or preparation exception, when one exists. */
    public Throwable exception() { return exception; }
    /** Returns a redacted diagnostic summary. */
    public String message() { return message; }
    /** Returns whether the receiver returned a {@code 2xx} response. */
    public boolean isUploaded() { return status == ReportUploadStatus.UPLOADED; }

    /** Returns this result on success, otherwise throws a safe exception without changing the finalized session. */
    public ReportUploadResult requireSuccess() {
        if (!isUploaded()) throw new ReportUploadException(this);
        return this;
    }

    String failureMessage() {
        return "Report upload " + status.name().toLowerCase(java.util.Locale.ROOT) + " for " + endpoint
                + (failureCategory == ReportUploadFailureCategory.NONE ? "" : " (" + failureCategory + ")")
                + (message.isBlank() ? "" : ": " + message);
    }

    @Override public String toString() {
        return "ReportUploadResult[status=" + status + ", artifactKind=" + artifactKind + ", endpoint=" + endpoint
                + ", httpStatus=" + httpStatus + ", attempts=" + attempts + ", elapsed=" + elapsed
                + ", payloadSize=" + payloadSize + ", sha256=" + sha256 + ", serverReportId=" + serverReportId
                + ", failureCategory=" + failureCategory + "]";
    }
}
