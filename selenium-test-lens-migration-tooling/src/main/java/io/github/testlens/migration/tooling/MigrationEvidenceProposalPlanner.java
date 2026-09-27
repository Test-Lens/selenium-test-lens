package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Conservative structured-code mapping from validated evidence to non-patch review proposals. */
public final class MigrationEvidenceProposalPlanner {
    public List<MigrationProposal> plan(MigrationEvidence evidence){List<MigrationProposal>out=new ArrayList<>();var engine=new MigrationProposalEngine();for(var fact:evidence.facts()){String code=fact.code().toUpperCase(Locale.ROOT);if(code.contains("VIEWPORT"))out.add(engine.configurationProposal("ALIGN_VIEWPORT_AND_RERUN","current viewport","controlled viewport",List.of(evidence.ref())));else if(code.contains("DATASET"))out.add(engine.configurationProposal("ALIGN_DATASET_AND_RERUN","current dataset","controlled dataset",List.of(evidence.ref())));else if(code.contains("BROWSER")||code.contains("NOT_COMPARABLE"))out.add(engine.configurationProposal("ALIGN_COMPARISON_CONFIGURATION_AND_RERUN","current comparison configuration","controlled comparison configuration",List.of(evidence.ref())));else if(evidence.kind()==MigrationEvidence.Kind.STATIC_COMPATIBILITY&&(code.contains("ROBOT")||code.contains("NATIVE_FILE_DIALOG")||code.contains("TOOLKIT_SCREEN")))out.add(engine.advisoryProposal(MigrationProposal.Category.MANUAL_ONLY,MigrationProposal.Eligibility.MANUAL_ONLY,"REVIEW_INTERACTION_ASSUMPTION",fact.category(),fact.code(),List.of(evidence.ref()),MigrationProposal.CausalState.HYPOTHESIS,List.of("STATIC_EVIDENCE_CANNOT_AUTHORIZE_TRANSFORM")));else if(code.equals("NO_MEANINGFUL_DIFFERENCE"))out.add(engine.advisoryProposal(MigrationProposal.Category.CONFIGURATION,MigrationProposal.Eligibility.NO_CHANGE_RECOMMENDED,code,null,null,List.of(evidence.ref()),MigrationProposal.CausalState.CONFIRMED,List.of()));}return List.copyOf(out);}
}
