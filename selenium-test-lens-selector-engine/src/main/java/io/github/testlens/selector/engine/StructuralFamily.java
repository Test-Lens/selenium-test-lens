package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.List;

/** Privacy-preserving detector-backed shape used by similarity and digest-only history preview. */
public record StructuralFamily(int version,String strategy,String componentRef,List<Segment> segments,
                               String fingerprint,String prefixFingerprint) {
    public static final int VERSION=1;
    public StructuralFamily{segments=List.copyOf(segments);}
    public static StructuralFamily from(SelectorSubject subject,ComponentIdentity component,PatternProposal.Result proposal){
        if(proposal==null||!proposal.useful()||!subject.hasTrustedExactValue())return null;
        List<Segment> shape=new ArrayList<>();List<String> fields=new ArrayList<>(List.of(Integer.toString(VERSION),subject.strategy(),component.componentRef()));
        String prefix=null;
        for(SelectorPolicy.Segment s:proposal.pattern().segments()){
            Segment segment;if(s.kind()==SelectorPolicy.SegmentKind.LITERAL){int length=s.literal().codePointCount(0,s.literal().length());String digest="sha256:"+CanonicalDigests.digest("selector-family-literal-v1",s.literal());segment=new Segment(Kind.LITERAL_DIGEST,length,digest);if(prefix==null&&length>0)prefix="selector-prefix-v1:sha256:"+CanonicalDigests.digest("selector-prefix-v1",subject.strategy(),component.componentRef(),Integer.toString(length),digest);}
            else segment=new Segment(Kind.valueOf(s.kind().name()),s.minLength(),null);
            shape.add(segment);fields.add(segment.kind().name());fields.add(Integer.toString(segment.length()));fields.add(segment.literalDigest()==null?"":segment.literalDigest());
        }
        return new StructuralFamily(VERSION,subject.strategy(),component.componentRef(),shape,"selector-family-v1:sha256:"+CanonicalDigests.digest("selector-family-v1",fields.toArray(String[]::new)),prefix);
    }
    public Match matchDigestOnly(SelectorPolicy.StructuralPattern pattern){
        if(pattern==null||!strategy.equals(pattern.strategy())||pattern.segments().size()!=segments.size())return Match.NO_MATCH;
        for(int i=0;i<segments.size();i++){Segment stored=segments.get(i);SelectorPolicy.Segment wanted=pattern.segments().get(i);
            if(wanted.kind()==SelectorPolicy.SegmentKind.OPAQUE_RUN)return Match.UNSUPPORTED_FROM_DIGEST_ONLY;
            if(wanted.kind()==SelectorPolicy.SegmentKind.LITERAL){if(stored.kind()!=Kind.LITERAL_DIGEST)return Match.NO_MATCH;String digest="sha256:"+CanonicalDigests.digest("selector-family-literal-v1",wanted.literal());if(stored.length()!=wanted.literal().codePointCount(0,wanted.literal().length())||!digest.equals(stored.literalDigest()))return Match.NO_MATCH;}
            else if(!stored.kind().name().equals(wanted.kind().name())||stored.length()<wanted.minLength()||stored.length()>wanted.maxLength())return Match.NO_MATCH;}
        return Match.MATCH;
    }
    public enum Kind{LITERAL_DIGEST,UUID_LIKE,DECIMAL_RUN,HEX_RUN}
    public enum Match{MATCH,NO_MATCH,UNSUPPORTED_FROM_DIGEST_ONLY}
    public record Segment(Kind kind,int length,String literalDigest){}
}
