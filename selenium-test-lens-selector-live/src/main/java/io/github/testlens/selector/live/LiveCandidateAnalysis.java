package io.github.testlens.selector.live;

import io.github.testlens.selector.engine.CandidateAnalysis;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ephemeral live result. Element handles are bounded, never serialized, and cleared on close. */
public final class LiveCandidateAnalysis implements AutoCloseable {
    public static final int MAX_RETAINED_PER_CANDIDATE=20;
    public static final int MAX_TOTAL_RETAINED=200;
    private final CandidateAnalysis analysis;
    private final String analysisId;
    private WebElement target;
    private final String documentGeneration;
    private final Map<String,List<WebElement>>matches;
    private final Set<String>notRetained;
    private final int retainedCount;

    LiveCandidateAnalysis(CandidateAnalysis analysis,String analysisId,WebElement target,String documentGeneration,
                          Map<String,List<WebElement>>matches,Set<String>notRetained,int retainedCount){
        this.analysis=analysis;this.analysisId=analysisId;this.target=target;this.documentGeneration=documentGeneration;
        this.matches=new LinkedHashMap<>(matches);this.notRetained=Set.copyOf(notRetained);this.retainedCount=retainedCount;
    }
    public CandidateAnalysis analysis(){return analysis;}
    public String analysisId(){return analysisId;}
    public WebElement target(){return target;}
    public String documentGeneration(){return documentGeneration;}
    public List<WebElement>retainedMatches(String candidateId){return matches.getOrDefault(candidateId,List.of());}
    public boolean matchesNotRetainedForHighlight(String candidateId){return notRetained.contains(candidateId);}
    public int retainedCount(){return retainedCount;}
    public boolean closed(){return target==null;}
    @Override public void close(){matches.clear();target=null;}
}
