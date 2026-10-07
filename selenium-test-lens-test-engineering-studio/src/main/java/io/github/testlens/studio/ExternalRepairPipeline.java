package io.github.testlens.studio;

import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.EvidenceFailureClassifier;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.SelectorRepairPlanner;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.application.tooling.source.CorrelationOverrides;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.PageObjectCorrelator;
import io.github.testlens.application.tooling.source.SourceImpact;
import io.github.testlens.application.tooling.source.SourceImpactAnalyzer;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.BrowserSession;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Bridges Studio execution evidence to the existing S12/S13 classification and repair pipeline. */
final class ExternalRepairPipeline {
    private static final RedactionPolicy REDACTION = RedactionPolicy.defaults();
    private final Path projectRoot;
    private final List<Path> sourceRoots;
    private final List<Path> classpath;
    private final BrowserSessionProvider browsers;
    private final BrowserRequest mappingRequest;
    private final URI startUrl;
    private Analysis lastAnalysis;
    private String lastAnalysisFailure;

    ExternalRepairPipeline(Path projectRoot,List<Path> sourceRoots,List<Path> classpath,
                           BrowserSessionProvider browsers,BrowserRequest mappingRequest,URI startUrl) {
        this.projectRoot=projectRoot;this.sourceRoots=List.copyOf(sourceRoots);this.classpath=List.copyOf(classpath);
        this.browsers=browsers;this.mappingRequest=mappingRequest;this.startUrl=startUrl;
    }

    AgentWorkflowCoordinator.FailureClassifier classifier(){return this::classify;}
    AgentWorkflowCoordinator.Stabilizer stabilizer(){return this::propose;}

    private synchronized FailureClassification classify(TestExecutionResult execution) {
        List<String> executionEvidence=evidence(execution);
        boolean productMismatch=executionEvidence.stream().anyMatch(ExternalRepairPipeline::isAssertionFailure);
        Analysis analysis=null;
        lastAnalysisFailure=null;
        if(!productMismatch&&executionEvidence.stream().anyMatch(ExternalRepairPipeline::isMissingElement)){
            try{analysis=analyze(execution,executionEvidence);}
            catch(Exception failure){analysis=null;lastAnalysisFailure=boundedFailure(failure);}
        }
        lastAnalysis=analysis;
        List<String> selectorEvidence=analysis==null?List.of():analysis.selectorEvidence();
        FailureClassification result=new EvidenceFailureClassifier().classify(new EvidenceFailureClassifier.Signals(
                false,List.of(),false,List.of(),productMismatch,
                productMismatch?executionEvidence:List.of(),analysis!=null,selectorEvidence,List.of(),false,List.of(),
                analysis==null?List.of():List.of(analysis.before().elementId())));
        if(result.category()==FailureClassification.Category.UNKNOWN&&lastAnalysisFailure!=null){
            List<String>counter=new ArrayList<>(result.counterEvidenceRefs());counter.add(lastAnalysisFailure);
            return new FailureClassification(result.header(),result.category(),result.rootCause(),result.affectedIds(),result.evidenceRefs(),counter);
        }
        return result;
    }

    private synchronized Optional<RepairProposal> propose(FailureClassification classification,
                                                            TestExecutionResult execution) {
        Analysis analysis=lastAnalysis;lastAnalysis=null;
        if(classification.category()!=FailureClassification.Category.SELECTOR_INSTABILITY||analysis==null)return Optional.empty();
        return Optional.of(new SelectorRepairPlanner().propose(
                "repair-"+Integer.toUnsignedString(Objects.hash(execution.scenarioId(),analysis.source().sourceDeclarationRef()),36),
                classification,analysis.before(),analysis.after(),analysis.source(),analysis.impact(),analysis.index(),
                "execution:"+execution.scenarioId(),"mapping:live-selector-drift"));
    }

    private Analysis analyze(TestExecutionResult execution,List<String> executionEvidence)throws Exception{
        ApplicationModel baseline=baselineModel();
        ExistingProjectIndex index=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                projectRoot,sourceRoots,classpath));
        PageObjectCorrelation correlation=new PageObjectCorrelator().correlate(baseline,index,CorrelationOverrides.none());
        ApplicationModel changed=mapCurrentApplication();
        for(ApplicationModel.ElementModel before:elements(baseline)){
            ApplicationModel.ElementModel after=replacementTarget(before,changed);
            if(!verifiedReplacement(before,after))continue;
            PageObjectCorrelation.ElementCorrelation source=correlation.elements().stream()
                    .filter(value->value.applicationElementId().equals(before.elementId()))
                    .filter(value->value.sourceDeclarationRef()!=null)
                    .filter(value->value.state()==PageObjectCorrelation.State.EXACT||value.state()==PageObjectCorrelation.State.STRONG)
                    .findFirst().orElse(null);
            if(source==null||!failureIdentifiesOldSelector(executionEvidence,before.preferredSelector()))continue;
            SourceImpact impact=new SourceImpactAnalyzer().analyze(before.elementId(),correlation,index,100);
            List<String> selectorEvidence=new ArrayList<>(executionEvidence);
            selectorEvidence.add("correlated source declaration: "+source.sourceDeclarationRef());
            selectorEvidence.add("old candidate no longer resolves: "+before.preferredSelector().candidateId());
            selectorEvidence.add("replacement live validation: "+after.preferredSelector().validation());
            selectorEvidence.add("replacement same target: "+after.preferredSelector().sameTarget());
            selectorEvidence.add("replacement unique: "+after.preferredSelector().unique());
            return new Analysis(before,after,source,impact,index,List.copyOf(selectorEvidence));
        }
        return null;
    }

    private ApplicationModel baselineModel()throws Exception{
        Object projection=new StudioWorkspaceStore(projectRoot).read(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL)
                .orElseThrow(()->new IllegalStateException("A persisted baseline application model is required"))
                .get("projection");
        return new ApplicationModelJson().read(StrictJson.write(projection));
    }

    private ApplicationModel mapCurrentApplication()throws Exception{
        try(BrowserSession session=browsers.open(mappingRequest)){
            if(startUrl!=null)session.driver().get(startUrl.toString());
            ApplicationMapper mapper=ApplicationMapper.start(session.driver(),
                    ApplicationMapperOptions.builder("Studio repair analysis").build());
            mapper.observe();return mapper.model();
        }
    }

    private static boolean verifiedReplacement(ApplicationModel.ElementModel before,ApplicationModel.ElementModel after){
        if(before==null||after==null||before.preferredSelector()==null||after.preferredSelector()==null)return false;
        ApplicationModel.SelectorProjection replacement=after.preferredSelector();
        return !Objects.equals(before.preferredSelector().value(),replacement.value())
                &&replacement.source()==ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS
                &&"VERIFIED_IN_SCOPE".equalsIgnoreCase(replacement.validation())
                &&"SAME_TARGET".equalsIgnoreCase(replacement.sameTarget())&&replacement.unique();
    }

    private static boolean failureIdentifiesOldSelector(List<String> evidence,ApplicationModel.SelectorProjection old){
        String value=old.value().toLowerCase(java.util.Locale.ROOT);
        List<String> identifiers=java.util.regex.Pattern.compile("[a-z0-9_-]{4,}")
                .matcher(value).results().map(java.util.regex.MatchResult::group)
                .filter(token->!token.equals("css")&&!token.equals("xpath")).distinct().toList();
        return evidence.stream().map(item->item.toLowerCase(java.util.Locale.ROOT).replace("\\",""))
                .anyMatch(item->item.contains(value.replace("\\",""))||identifiers.stream().anyMatch(item::contains));
    }
    private static ApplicationModel.ElementModel replacementTarget(ApplicationModel.ElementModel before,ApplicationModel changed){
        List<ApplicationModel.ElementModel> values=elements(changed);
        ApplicationModel.ElementModel exact=values.stream().filter(value->value.elementId().equals(before.elementId())).findFirst().orElse(null);
        if(exact!=null)return exact;
        List<ApplicationModel.ElementModel> semantic=values.stream()
                .filter(value->value.semanticName().equals(before.semanticName()))
                .filter(value->value.type()==before.type())
                .filter(value->Objects.equals(value.semanticRole(),before.semanticRole())).toList();
        if(semantic.size()==1)return semantic.get(0);
        List<ApplicationModel.ElementModel> accessible=values.stream()
                .filter(value->before.accessibleName()!=null&&!before.accessibleName().isBlank())
                .filter(value->Objects.equals(value.accessibleName(),before.accessibleName()))
                .filter(value->value.type()==before.type())
                .filter(value->Objects.equals(value.semanticRole(),before.semanticRole())).toList();
        if(accessible.size()==1)return accessible.get(0);
        List<ApplicationModel.ElementModel> labelled=values.stream()
                .filter(value->before.label()!=null&&!before.label().isBlank())
                .filter(value->Objects.equals(value.label(),before.label()))
                .filter(value->value.type()==before.type())
                .filter(value->Objects.equals(value.semanticRole(),before.semanticRole())).toList();
        return labelled.size()==1?labelled.get(0):null;
    }
    private static boolean isMissingElement(String value){return value.contains("NoSuchElementException")||value.contains("no such element");}
    private static boolean isAssertionFailure(String value){return value.contains("AssertionFailedError")||value.contains("AssertionError");}
    private static String boundedFailure(Exception failure){String value=REDACTION.redact("Selector repair analysis failed: "+failure.getClass().getSimpleName()+": "+String.valueOf(failure.getMessage()));return value.length()<=500?value:value.substring(0,500);}
    private static List<ApplicationModel.ElementModel> elements(ApplicationModel model){return model.pages().stream().flatMap(page->page.elements().stream()).toList();}
    private static List<String> evidence(TestExecutionResult execution){List<String> values=new ArrayList<>();values.addAll(execution.traceEvidenceRefs());values.addAll(execution.assertionEvidenceRefs());values.addAll(execution.runtimeEventRefs());values.addAll(execution.selectorDiagnosticRefs());return List.copyOf(values);}

    private record Analysis(ApplicationModel.ElementModel before,ApplicationModel.ElementModel after,
                            PageObjectCorrelation.ElementCorrelation source,SourceImpact impact,
                            ExistingProjectIndex index,List<String> selectorEvidence){}
}
