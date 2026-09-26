package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Conservative detector-backed structural-pattern proposal; never persists a policy. */
public final class PatternProposal {
    public static final int SCHEMA_VERSION=1;
    private final AppearanceClassifier classifier=new AppearanceClassifier();

    public Result propose(SelectorSubject subject) {
        if (!subject.hasTrustedExactValue()) return new Result(SCHEMA_VERSION,null,List.of(),List.of(),List.of("TRUSTED_CANONICAL_VALUE_REQUIRED"));
        List<AppearanceSignal> findings=classifier.classify(subject);
        List<Part> supported=findings.stream().map(PatternProposal::part).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(Part::specificity).reversed().thenComparingInt(p->-(p.end-p.start))
                        .thenComparingInt(Part::start).thenComparing(Part::detector)).toList();
        List<Part> chosen=new ArrayList<>();List<String> suppressed=new ArrayList<>();
        for(Part candidate:supported){if(chosen.stream().anyMatch(p->candidate.start<p.end&&p.start<candidate.end))suppressed.add(candidate.detector);else chosen.add(candidate);}
        chosen.sort(Comparator.comparingInt(Part::start));
        if(chosen.isEmpty())return new Result(SCHEMA_VERSION,null,findings,suppressed,List.of("NO_SUPPORTED_PLACEHOLDER"));
        String value=subject.canonicalValue();int[] cps=value.codePoints().toArray();int cursor=0;List<SelectorPolicy.Segment> segments=new ArrayList<>();
        for(Part p:chosen){if(p.start>cursor)segments.add(SelectorPolicy.Segment.literal(new String(cps,cursor,p.start-cursor)));
            int length=p.end-p.start;segments.add(SelectorPolicy.Segment.run(p.kind,length,length));cursor=p.end;}
        if(cursor<cps.length)segments.add(SelectorPolicy.Segment.literal(new String(cps,cursor,cps.length-cursor)));
        return new Result(SCHEMA_VERSION,new SelectorPolicy.StructuralPattern(subject.strategy(),segments),findings,suppressed,List.of());
    }

    private static Part part(AppearanceSignal s){return switch(s.family()){
        case UUID_LIKE -> new Part(s.startCodePoint(),s.endCodePoint(),SelectorPolicy.SegmentKind.UUID_LIKE,3,s.detector());
        case EPOCH_TIMESTAMP_LIKE,LONG_NUMERIC_SEQUENCE_LIKE -> new Part(s.startCodePoint(),s.endCodePoint(),SelectorPolicy.SegmentKind.DECIMAL_RUN,2,s.detector());
        case HEX_HASH_FRAGMENT_LIKE -> new Part(s.startCodePoint(),s.endCodePoint(),SelectorPolicy.SegmentKind.HEX_RUN,1,s.detector());
        default -> null;};}
    private record Part(int start,int end,SelectorPolicy.SegmentKind kind,int specificity,String detector){}
    public record Result(int schemaVersion,SelectorPolicy.StructuralPattern pattern,List<AppearanceSignal> findings,
                         List<String> suppressedDetectorIds,List<String> issues){public Result{findings=List.copyOf(findings);suppressedDetectorIds=List.copyOf(suppressedDetectorIds);issues=List.copyOf(issues);}public boolean useful(){return pattern!=null&&pattern.segments().stream().anyMatch(s->s.kind()!=SelectorPolicy.SegmentKind.LITERAL);}}
}
