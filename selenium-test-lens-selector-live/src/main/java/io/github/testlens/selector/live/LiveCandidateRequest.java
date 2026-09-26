package io.github.testlens.selector.live;

import io.github.testlens.selector.engine.CandidateAnalysis;
import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.ObservationEvidence;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Internal explicit request. No instance is created by normal Test Lens runtime. */
public record LiveCandidateRequest(WebDriver driver, SearchContext searchContext, WebElement target,
                                   CandidateAnalysis.UsageIntent usageIntent, By originalBy,
                                   String contextFingerprint, boolean shadowContext,
                                   String declarationRef, String modulePath, String logicalPath,
                                   String declaringSymbol, String usageClass, String usageMethod,
                                   CompiledPolicySet policies, ObservationEvidence evidence,
                                   List<String> additionalPreferredTestAttributes) {
    public LiveCandidateRequest {
        Objects.requireNonNull(driver,"driver");
        usageIntent=usageIntent==null?CandidateAnalysis.UsageIntent.UNKNOWN:usageIntent;
        policies=policies==null?CompiledPolicySet.empty():policies;
        evidence=evidence==null?ObservationEvidence.unavailable():evidence;
        LinkedHashSet<String> names=new LinkedHashSet<>();names.add("data-testid");
        if(additionalPreferredTestAttributes!=null)for(String name:additionalPreferredTestAttributes){
            if(name==null||!name.matches("[A-Za-z_][A-Za-z0-9_.:-]{0,63}"))throw new IllegalArgumentException("Unsafe test attribute name: "+name);
            names.add(name);
        }
        if(names.size()>8)throw new IllegalArgumentException("At most 8 preferred test attributes are supported");
        additionalPreferredTestAttributes=List.copyOf(names);
    }
}
