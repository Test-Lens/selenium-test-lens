package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.CanonicalDigests;
import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.FederatedPatternPreview;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.testlens.selector.tooling.SelectorAuditModel.*;

/** Tooling-only file input boundary. Optional malformed inputs degrade coverage; the selector index is mandatory. */
final class SelectorAuditRunner {
    AuditReport audit(Path selectorIndex,Path policy,Path history,List<SelectorAuditOrchestrator.CandidateInput>candidates,
                      String projectFingerprint,Map<String,String>currentContentHashes,
                      Map<String,FederatedPatternPreview.Result>previews)throws IOException{
        SelectorIndexModel.SelectorIndex index;String indexFingerprint;
        try{byte[]bytes=bounded(selectorIndex,SelectorIndexJson.MAX_DOCUMENT_BYTES);indexFingerprint="selector-audit-input-index-v1:sha256:"+CanonicalDigests.digestBytes("selector-audit-input-index-v1",bytes);index=SelectorIndexJson.read(selectorIndex);}catch(Exception failure){throw new SelectorAuditOrchestrator.AuditInputException("MALFORMED_REQUIRED_SELECTOR_INDEX: "+failure.getMessage());}
        List<ProjectIssue>issues=new ArrayList<>();CompiledPolicySet policies=CompiledPolicySet.empty();String policyFingerprint=null;
        if(policy!=null)try{byte[]bytes=bounded(policy,SelectorPolicyJson.MAX_DOCUMENT_BYTES);policyFingerprint="selector-audit-input-policy-v1:sha256:"+CanonicalDigests.digestBytes("selector-audit-input-policy-v1",bytes);policies=CompiledPolicySet.compile(SelectorPolicyJson.read(policy));}catch(Exception failure){issues.add(SelectorAuditOrchestrator.issue(ProjectIssueCode.MALFORMED_OPTIONAL_POLICY,Severity.REVIEW,List.of("OPTIONAL_POLICY_REJECTED"),List.of(),"Optional policy input was rejected; Audit continued without policies"));}
        SelectorHistoryModel.History historyModel=null;String historyFingerprint=null;
        if(history!=null)try{byte[]bytes=bounded(history,SelectorHistoryJson.MAX_DOCUMENT_BYTES);historyFingerprint="selector-audit-input-history-v1:sha256:"+CanonicalDigests.digestBytes("selector-audit-input-history-v1",bytes);historyModel=SelectorHistoryJson.read(history);}catch(Exception failure){issues.add(SelectorAuditOrchestrator.issue(ProjectIssueCode.MALFORMED_OPTIONAL_HISTORY,Severity.REVIEW,List.of("OPTIONAL_HISTORY_REJECTED"),List.of(),"Optional history input was rejected; Audit continued without history"));}
        SelectorAuditOrchestrator.Request request=new SelectorAuditOrchestrator.Request(index,policies,historyModel,candidates,projectFingerprint,currentContentHashes,previews,indexFingerprint,historyFingerprint,policyFingerprint,issues);
        return new SelectorAuditOrchestrator().audit(request);
    }
    private static byte[]bounded(Path path,int limit)throws IOException{long size=Files.size(path);if(size>limit)throw new IOException("Input exceeds hard document limit");return Files.readAllBytes(path);}
}
