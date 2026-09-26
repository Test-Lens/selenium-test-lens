package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.SelectorAuditProjection;

import java.util.ArrayList;
import java.util.List;

/** Internal tooling adapter from the canonical sanitized Audit model to the neutral Lab projection. */
public final class SelectorAuditProjectionAdapter {
    private SelectorAuditProjectionAdapter(){}
    public static SelectorAuditProjection project(SelectorAuditModel.AuditReport report){
        List<SelectorAuditProjection.Declaration>declarations=new ArrayList<>();
        for(SelectorAuditModel.DeclarationAudit value:report.declarations()){
            SelectorAuditModel.SourceRef source=value.source();
            List<SelectorAuditProjection.Finding>findings=value.findings().stream().map(f->
                    new SelectorAuditProjection.Finding(f.severity().name(),f.state().name(),f.code().name(),f.reasonCodes())).toList();
            SelectorAuditProjection.Summary evidence=new SelectorAuditProjection.Summary(value.evidenceSummary().state(),
                    value.evidenceSummary().limitations(),value.evidenceSummary().incomplete());
            SelectorAuditProjection.Summary policy=new SelectorAuditProjection.Summary(
                    String.join(",",value.policySummary().decisions()),value.policySummary().selectedRuleIds(),value.policySummary().conflictCount()>0);
            declarations.add(new SelectorAuditProjection.Declaration(value.declarationRef(),
                    new SelectorAuditProjection.Source(source.logicalPath(),source.startLine(),source.startColumn(),source.endLine(),source.endColumn()),
                    findings,evidence,policy,value.primaryRecommendation().name(),value.similarSummary().familyFingerprints(),value.limitations()));
        }
        List<String>coverage=new ArrayList<>(report.outputLimitations());
        if(report.overallCompleteness()!=SelectorAuditModel.CompletenessStatus.COMPLETE_FOR_REQUESTED_INPUTS)coverage.add("AUDIT_"+report.overallCompleteness().name());
        return new SelectorAuditProjection(SelectorAuditProjection.SCHEMA_VERSION,declarations,coverage.stream().distinct().sorted().toList());
    }
}
