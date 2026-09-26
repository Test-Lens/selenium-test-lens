package io.github.testlens.selector.engine;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable internal policy schema V1. */
public final class SelectorPolicy {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_RULES = 10_000;
    public static final int MAX_SEGMENTS = 32;
    public static final int MAX_LITERAL_CODE_POINTS = 512;
    public static final int MAX_RUN_LENGTH = 4_096;
    public static final int MAX_NOTE_CODE_POINTS = 2_048;
    private SelectorPolicy() { }

    public record Document(int schemaVersion, int canonicalizationVersion, List<Rule> rules) {
        public Document {
            if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported policy schemaVersion: " + schemaVersion);
            if (canonicalizationVersion != CanonicalDigests.CANONICALIZATION_VERSION) throw new IllegalArgumentException("Unsupported canonicalizationVersion: " + canonicalizationVersion);
            rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
            if (rules.size() > MAX_RULES) throw new IllegalArgumentException("Too many policy rules");
        }
        public static Document empty() { return new Document(SCHEMA_VERSION, CanonicalDigests.CANONICALIZATION_VERSION, List.of()); }
        public Document canonical() { return new Document(schemaVersion, canonicalizationVersion, rules.stream().sorted(Comparator.comparing(Rule::ruleId)).toList()); }
    }

    public record Rule(String ruleId, Decision decision, int priority, Scope scope, Matcher matcher, Reason reason) {
        public Rule {
            Objects.requireNonNull(ruleId); Objects.requireNonNull(decision); Objects.requireNonNull(scope); Objects.requireNonNull(matcher);
            if (priority < -1_000_000 || priority > 1_000_000) throw new IllegalArgumentException("priority outside supported range");
            if (!ruleId.equals(calculateRuleId(decision, priority, scope, matcher))) throw new IllegalArgumentException("ruleId does not match semantic content");
            reason = reason == null ? new Reason("USER_DECLARATION", null) : reason;
        }
        public static Rule create(Decision decision, int priority, Scope scope, Matcher matcher, Reason reason) {
            return new Rule(calculateRuleId(decision, priority, scope, matcher), decision, priority, scope, matcher, reason);
        }
    }

    public enum Decision { STABLE, UNSTABLE }
    public enum MatcherKind { EXACT_VALUE_DIGEST, STRUCTURAL_PATTERN }

    public sealed interface Matcher permits ExactMatcher, StructuralPattern {
        MatcherKind kind();
        String strategy();
        boolean matches(SelectorSubject subject);
        String canonicalForm();
        default int matcherSpecificity() { return kind() == MatcherKind.EXACT_VALUE_DIGEST ? 2 : 1; }
        default int literalCoverage() { return 0; }
        default int placeholderCount() { return 0; }
    }

    public record ExactMatcher(String strategy, String valueDigest, String displayHint) implements Matcher {
        public ExactMatcher {
            strategy = SelectorSubject.normalizeStrategy(strategy);
            if (strategy.isBlank()) throw new IllegalArgumentException("Exact matcher strategy is required");
            if (valueDigest == null || !valueDigest.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("valueDigest must be 64 lowercase hex characters");
            validateOptionalText(displayHint, 256, "displayHint");
        }
        public static ExactMatcher from(SelectorSubject subject) { return new ExactMatcher(subject.strategy(), CanonicalDigests.exactValueDigest(subject), null); }
        @Override public MatcherKind kind() { return MatcherKind.EXACT_VALUE_DIGEST; }
        @Override public boolean matches(SelectorSubject subject) {
            return subject.hasTrustedExactValue() && strategy.equals(subject.strategy()) && valueDigest.equals(CanonicalDigests.exactValueDigest(subject));
        }
        @Override public String canonicalForm() { return "exact|" + strategy + "|" + valueDigest; }
    }

    public record StructuralPattern(String strategy, List<Segment> segments) implements Matcher {
        public StructuralPattern {
            strategy = SelectorSubject.normalizeStrategy(strategy);
            if (strategy.isBlank()) throw new IllegalArgumentException("Pattern strategy is required");
            segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
            if (segments.isEmpty() || segments.size() > MAX_SEGMENTS) throw new IllegalArgumentException("Pattern requires 1.." + MAX_SEGMENTS + " segments");
        }
        @Override public MatcherKind kind() { return MatcherKind.STRUCTURAL_PATTERN; }
        @Override public boolean matches(SelectorSubject subject) {
            return subject.valueState() == SelectorSubject.ValueState.KNOWN
                    && subject.inputTrust() != SelectorSubject.InputTrust.RUNTIME_REDACTED
                    && strategy.equals(subject.strategy())
                    && matchSegments(subject.canonicalValue(), segments);
        }
        @Override public String canonicalForm() { return "pattern|" + strategy + "|" + segments.stream().map(Segment::canonicalForm).reduce((a,b)->a+"|"+b).orElse(""); }
        @Override public int literalCoverage() { return segments.stream().filter(s->s.kind()==SegmentKind.LITERAL).mapToInt(s->s.literal().codePointCount(0,s.literal().length())).sum(); }
        @Override public int placeholderCount() { return (int) segments.stream().filter(s->s.kind()!=SegmentKind.LITERAL).count(); }
    }

    public record Segment(SegmentKind kind, String literal, int minLength, int maxLength, String alphabet) {
        public Segment {
            Objects.requireNonNull(kind);
            if (kind == SegmentKind.LITERAL) {
                Objects.requireNonNull(literal, "literal");
                if (literal.codePointCount(0,literal.length()) > MAX_LITERAL_CODE_POINTS) throw new IllegalArgumentException("Literal segment too long");
                minLength = maxLength = literal.codePointCount(0,literal.length()); alphabet = null;
            } else {
                if (minLength < 1 || maxLength < minLength || maxLength > MAX_RUN_LENGTH) throw new IllegalArgumentException("Invalid run bounds");
                literal = null;
                if (kind == SegmentKind.OPAQUE_RUN) {
                    if (alphabet == null || alphabet.isBlank() || alphabet.codePointCount(0,alphabet.length()) > 128) throw new IllegalArgumentException("OPAQUE_RUN requires a bounded alphabet");
                    if (alphabet.codePoints().distinct().count() != alphabet.codePointCount(0,alphabet.length())) throw new IllegalArgumentException("OPAQUE_RUN alphabet contains duplicates");
                } else alphabet = null;
            }
        }
        public static Segment literal(String value) { return new Segment(SegmentKind.LITERAL, value, 0, 0, null); }
        public static Segment run(SegmentKind kind, int min, int max) { return new Segment(kind, null, min, max, null); }
        public static Segment opaque(String alphabet, int min, int max) { return new Segment(SegmentKind.OPAQUE_RUN, null, min, max, alphabet); }
        String canonicalForm() { return kind+":"+(literal==null?"":literal)+":"+minLength+":"+maxLength+":"+(alphabet==null?"":alphabet); }
    }
    public enum SegmentKind { LITERAL, UUID_LIKE, DECIMAL_RUN, HEX_RUN, OPAQUE_RUN }

    public record Scope(String modulePath, String logicalPath, String declarationRef, String declaringSymbol,
                        String usageClass, String usageMethod, String contextFingerprint) {
        public Scope {
            modulePath=path(modulePath); logicalPath=path(logicalPath);
            if (contextFingerprint != null && !contextFingerprint.matches("context-v1:sha256:[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid contextFingerprint");
        }
        public static Scope project() { return new Scope(null,null,null,null,null,null,null); }
        public boolean matches(SelectorSubject s) {
            return eq(modulePath,s.modulePath()) && eq(logicalPath,s.logicalPath()) && eq(declarationRef,s.declarationRef())
                    && eq(declaringSymbol,s.declaringSymbol()) && eq(usageClass,s.usageClass()) && eq(usageMethod,s.usageMethod())
                    && eq(contextFingerprint,s.contextFingerprint());
        }
        int specificity() {
            if (declarationRef != null) return 500;
            if (contextFingerprint != null || declaringSymbol != null || usageClass != null || usageMethod != null) return 400;
            if (logicalPath != null) return 300;
            if (modulePath != null) return 200;
            return 100;
        }
        String canonicalForm() { return String.join("|", n(modulePath),n(logicalPath),n(declarationRef),n(declaringSymbol),n(usageClass),n(usageMethod),n(contextFingerprint)); }
        private static boolean eq(String wanted,String actual){ return wanted==null || wanted.equals(actual); }
        private static String n(String s){return s==null?"":s;}
        private static String path(String s){return s==null?null:s.replace('\\','/');}
    }

    public record Reason(String code, String note) {
        public Reason { if(code==null||code.isBlank()) throw new IllegalArgumentException("reason code required"); validateOptionalText(note,MAX_NOTE_CODE_POINTS,"note"); }
    }

    private static String calculateRuleId(Decision d,int p,Scope s,Matcher m) {
        List<String> fields=new ArrayList<>();
        fields.add("rule-v1");fields.add(d.name());fields.add(Integer.toString(p));
        fields.addAll(List.of(n(s.modulePath()),n(s.logicalPath()),n(s.declarationRef()),n(s.declaringSymbol()),
                n(s.usageClass()),n(s.usageMethod()),n(s.contextFingerprint())));
        fields.add(m.kind().name());fields.add(m.strategy());
        if(m instanceof ExactMatcher exact){fields.add(exact.valueDigest());}
        else if(m instanceof StructuralPattern pattern)for(Segment segment:pattern.segments()){
            fields.add(segment.kind().name());fields.add(n(segment.literal()));fields.add(Integer.toString(segment.minLength()));
            fields.add(Integer.toString(segment.maxLength()));fields.add(n(segment.alphabet()));
        }
        return "selector-policy-v1:sha256:"+CanonicalDigests.sha256(CanonicalDigests.lengthPrefixed(fields.toArray(String[]::new)));
    }
    private static String n(String value){return value==null?"":value;}
    private static void validateOptionalText(String v,int max,String name){if(v!=null&&v.codePointCount(0,v.length())>max)throw new IllegalArgumentException(name+" too long");}

    private static boolean matchSegments(String value,List<Segment> segments) {
        int[] cps=value.codePoints().toArray();
        return match(cps,0,segments,0,new java.util.HashSet<>());
    }
    private static boolean match(int[] cps,int pos,List<Segment> segments,int index,Set<Long> seen) {
        if(index==segments.size()) return pos==cps.length;
        long state=(((long)pos)<<32)|index; if(!seen.add(state))return false;
        Segment s=segments.get(index);
        if(s.kind()==SegmentKind.LITERAL){int[] lit=s.literal().codePoints().toArray();if(pos+lit.length>cps.length)return false;for(int i=0;i<lit.length;i++)if(cps[pos+i]!=lit[i])return false;return match(cps,pos+lit.length,segments,index+1,seen);}
        int max=Math.min(s.maxLength(),cps.length-pos);
        for(int len=s.minLength();len<=max;len++)if(accepts(s,cps,pos,len)&&match(cps,pos+len,segments,index+1,seen))return true;
        return false;
    }
    private static boolean accepts(Segment s,int[] cps,int pos,int len){
        String v=new String(cps,pos,len);
        return switch(s.kind()){
            case UUID_LIKE -> len==36&&v.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
            case DECIMAL_RUN -> v.codePoints().allMatch(Character::isDigit);
            case HEX_RUN -> v.codePoints().allMatch(cp->Character.digit(cp,16)>=0);
            case OPAQUE_RUN -> v.codePoints().allMatch(cp->s.alphabet().indexOf(cp)>=0);
            case LITERAL -> false;
        };
    }
}
