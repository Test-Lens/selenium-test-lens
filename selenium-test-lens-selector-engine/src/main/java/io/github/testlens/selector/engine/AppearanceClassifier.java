package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic detector catalog V1. Findings describe appearance, never provenance or failure probability. */
public final class AppearanceClassifier {
    public static final int DETECTOR_CATALOG_VERSION = 1;
    private static final Pattern UUID = Pattern.compile("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    private static final Pattern EPOCH_MILLIS = Pattern.compile("(?<!\\d)(?:1[3-9]|2[0-9])\\d{11}(?!\\d)");
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<!\\d)\\d{9,}(?!\\d)");
    private static final Pattern HEX = Pattern.compile("(?i)(?<![0-9a-f])[0-9a-f]{10,64}(?![0-9a-f])");
    private static final Pattern CSS_IN_JS = Pattern.compile("(?i)^css-[a-z0-9]{6,12}$");
    private static final Pattern CSS_MODULE = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*__[A-Za-z0-9_-]{5,12}$");
    private static final Pattern COUNTER = Pattern.compile("(?i)^(?:ember|ext|component|widget|react-select)-?[A-Za-z_-]*\\d{3,}$");
    private static final Pattern USE_ID = Pattern.compile("^:r[a-z0-9]+:$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTROPY_SUFFIX = Pattern.compile("(?i)(?:^|[-_])([a-z0-9]{12,24})$");

    public List<AppearanceSignal> classify(SelectorSubject subject) {
        if (subject.valueState() != SelectorSubject.ValueState.KNOWN || subject.canonicalValue() == null
                || subject.inputTrust() == SelectorSubject.InputTrust.RUNTIME_REDACTED) return List.of();
        String candidate = analyzableToken(subject);
        if (candidate == null || candidate.isBlank()) return List.of();
        List<AppearanceSignal> out = new ArrayList<>();
        matches(out, candidate, UUID, AppearanceSignal.Family.UUID_LIKE, AppearanceSignal.Confidence.HIGH, "uuid-shape-v1", "UUID_SHAPE", "UUID-like fragment detected");
        matches(out, candidate, EPOCH_MILLIS, AppearanceSignal.Family.EPOCH_TIMESTAMP_LIKE, AppearanceSignal.Confidence.HIGH, "epoch-millis-v1", "EPOCH_MILLIS_SHAPE", "Epoch-millisecond-like numeric fragment detected");
        matches(out, candidate, LONG_NUMBER, AppearanceSignal.Family.LONG_NUMERIC_SEQUENCE_LIKE, AppearanceSignal.Confidence.MEDIUM, "long-number-v1", "LONG_NUMERIC_RUN", "Long numeric sequence detected");
        matches(out, candidate, HEX, AppearanceSignal.Family.HEX_HASH_FRAGMENT_LIKE, AppearanceSignal.Confidence.MEDIUM, "hex-fragment-v1", "LONG_HEX_RUN", "Long hexadecimal/hash-like fragment detected");
        whole(out, candidate, CSS_IN_JS, AppearanceSignal.Family.CSS_IN_JS_TOKEN_LIKE, AppearanceSignal.Confidence.HIGH, "css-in-js-shape-v1", "CSS_IN_JS_SHAPE", "CSS-in-JS-like token detected");
        whole(out, candidate, CSS_MODULE, AppearanceSignal.Family.CSS_MODULE_TOKEN_LIKE, AppearanceSignal.Confidence.HIGH, "css-module-shape-v1", "CSS_MODULE_SHAPE", "CSS Modules-like token detected");
        whole(out, candidate, COUNTER, AppearanceSignal.Family.FRAMEWORK_COUNTER_ID_LIKE, AppearanceSignal.Confidence.MEDIUM, "framework-counter-shape-v1", "FRAMEWORK_COUNTER_SHAPE", "Framework counter-like identifier detected");
        whole(out, candidate, USE_ID, AppearanceSignal.Family.REACT_USEID_LIKE, AppearanceSignal.Confidence.MEDIUM, "react-useid-shape-v1", "REACT_USEID_TEXT_SHAPE", "React useId-like textual shape detected");
        Matcher entropy = ENTROPY_SUFFIX.matcher(candidate);
        if (entropy.find() && highEntropy(entropy.group(1))) add(out, candidate, entropy.start(1), entropy.end(1),
                AppearanceSignal.Family.HIGH_ENTROPY_SUFFIX_LIKE, AppearanceSignal.Confidence.MEDIUM,
                "entropy-suffix-v1", "HIGH_ENTROPY_SUFFIX", "High-entropy-looking suffix detected");
        int suspiciousStart = out.stream().mapToInt(AppearanceSignal::startCodePoint).filter(i -> i > 0).min().orElse(-1);
        if (suspiciousStart >= 3 && candidate.codePoints().limit(suspiciousStart).anyMatch(Character::isLetter)) {
            add(out, candidate, 0, candidate.length(), AppearanceSignal.Family.STABLE_PREFIX_SUSPICIOUS_SUFFIX,
                    AppearanceSignal.Confidence.HIGH, "mixed-prefix-suffix-v1", "READABLE_PREFIX_WITH_SUSPICIOUS_SUFFIX",
                    "Readable prefix combined with a generated-looking suffix");
        }
        return List.copyOf(out);
    }

    private static String analyzableToken(SelectorSubject s) {
        return switch (s.strategy()) {
            case "id", "name", "class name" -> s.canonicalValue();
            case "css selector" -> simpleCss(s.canonicalValue());
            case "xpath" -> null;
            default -> s.canonicalValue();
        };
    }
    private static String simpleCss(String v) { return v != null && v.matches("[.#][A-Za-z0-9_-]+") ? v.substring(1) : null; }
    private static boolean highEntropy(String v) {
        long letters = v.codePoints().filter(Character::isLetter).count(), digits = v.codePoints().filter(Character::isDigit).count();
        long distinct = v.toLowerCase(Locale.ROOT).codePoints().distinct().count();
        return letters >= 4 && digits >= 3 && distinct >= Math.min(10, v.length() - 2) && !v.matches("[A-Za-z]+[0-9]{1,4}");
    }
    private static void matches(List<AppearanceSignal> out, String v, Pattern p, AppearanceSignal.Family f,
                                AppearanceSignal.Confidence c, String d, String r, String e) { Matcher m=p.matcher(v); while(m.find()) add(out,v,m.start(),m.end(),f,c,d,r,e); }
    private static void whole(List<AppearanceSignal> out, String v, Pattern p, AppearanceSignal.Family f,
                              AppearanceSignal.Confidence c, String d, String r, String e) { if(p.matcher(v).find()) add(out,v,0,v.length(),f,c,d,r,e); }
    private static void add(List<AppearanceSignal> out, String v, int start, int end, AppearanceSignal.Family f,
                            AppearanceSignal.Confidence c, String d, String r, String e) {
        int cpStart=v.codePointCount(0,start), cpEnd=v.codePointCount(0,end);
        String digest="sha256:"+CanonicalDigests.digest("selector-fragment-v1",v.substring(start,end));
        out.add(new AppearanceSignal(f,c,d,1,"VALUE",cpStart,cpEnd,digest,r,e));
    }
}
