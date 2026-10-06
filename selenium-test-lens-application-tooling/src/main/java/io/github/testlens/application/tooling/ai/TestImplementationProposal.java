package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Test implementation proposal; the default contract forbids raw selectors. @since 0.5.0 */
public record TestImplementationProposal(ContractHeader header,String scenarioId,String sourcePatch,
                                         SelectorAccessPolicy selectorAccessPolicy,List<String>pageObjectApisUsed,
                                         List<PageObjectCapabilityMissing>missingCapabilities) {
    public TestImplementationProposal{if(header==null||selectorAccessPolicy==null)throw new IllegalArgumentException("header and selectorAccessPolicy are required");pageObjectApisUsed=canonical(pageObjectApisUsed);missingCapabilities=List.copyOf(missingCapabilities==null?List.of():missingCapabilities);}
    public record PageObjectCapabilityMissing(String pageId,String elementId,String requiredCapability,String reason){}
    public enum SelectorAccessPolicy{PAGE_OBJECTS_ONLY,RAW_SELECTORS_EXPLICITLY_ALLOWED}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
