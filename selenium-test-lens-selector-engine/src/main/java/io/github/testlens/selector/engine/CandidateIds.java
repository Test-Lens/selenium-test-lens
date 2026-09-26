package io.github.testlens.selector.engine;

public final class CandidateIds {
    private CandidateIds() { }
    public static String id(CandidateAnalysis.Locator locator,String context){return "selector-candidate-v1:sha256:"+CanonicalDigests.digest("selector-candidate-v1","1",locator.strategy(),locator.value(),context==null?"CURRENT_ANALYSIS_CONTEXT":context);}
    public static String opaqueOriginalId(){return "selector-candidate-v1:opaque-original";}
}
