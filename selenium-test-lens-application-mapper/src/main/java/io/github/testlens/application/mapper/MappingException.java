package io.github.testlens.application.mapper;

/** Typed mapper failure; code is stable and message is diagnostic only. @since 0.5.0 */
public final class MappingException extends RuntimeException {
    public enum Code { SCAN_LIMIT_REACHED, PAGE_IDENTITY_AMBIGUOUS, TARGET_STALE, UNSUPPORTED_CLOSED_SHADOW,
        SELECTOR_REVIEW_REQUIRED, MODEL_SERIALIZATION_FAILED, GENERATION_CONFLICT, EXISTING_SOURCE_CONFLICT,
        CRAWL_ACTION_BLOCKED, BROWSER_SCRIPT_FAILED, PAGE_STATE_UNRETAINED }
    private final Code code;
    public MappingException(Code code,String message){super(message);this.code=code;}
    public MappingException(Code code,String message,Throwable cause){super(message,cause);this.code=code;}
    public Code code(){return code;}
}
