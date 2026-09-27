package io.github.testlens.migration.tooling;

import io.github.testlens.selector.tooling.TrustedSelectorSourceProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationProposalEngineTest {
    @TempDir Path temporary;

    @Test void directLocatorProducesOnlyExpressionEditAndDeterministicDiff()throws Exception{
        String source="package p;\nimport org.openqa.selenium.By;\nclass Page {\n  // keep me\n  static final By SAVE = By.id(\"save\");\n}\n";
        Path java=write("src/p/Page.java",source.getBytes(StandardCharsets.UTF_8));
        var declaration=projection().declarations().get(0);var candidate=candidate(declaration,"css selector","[data-testid=\"save\"]");
        var planned=new MigrationProposalEngine().planLocatorWithPreview(input(declaration,candidate));var proposal=planned.proposal();String diff=new String(planned.exactPatchBytes(),StandardCharsets.UTF_8);
        assertEquals(MigrationProposal.Eligibility.READY_FOR_REVIEW,proposal.eligibility());
        assertEquals(1,proposal.sourceEdits().size());assertEquals("By.cssSelector(\"[data-testid=\\\"save\\\"]\")",proposal.sourceEdits().get(0).replacementText());
        assertTrue(diff.contains("-  static final By SAVE = By.id(\"save\");"));
        assertTrue(diff.contains("+  static final By SAVE = By.cssSelector(\"[data-testid=\\\"save\\\"]\");"));
        assertEquals(source,Files.readString(java));
        assertEquals(proposal.proposalId(),new MigrationProposalEngine().planLocator(input(declaration,candidate)).proposalId());
    }

    @Test void parameterizedRuntimeValueIsNeverLiteralized()throws Exception{
        write("src/p/Page.java",("package p; import org.openqa.selenium.By; class Page { By row(String rowId) { return By.id(\"row-\" + rowId); } }").getBytes(StandardCharsets.UTF_8));
        var declaration=projection().declarations().get(0);var proposal=new MigrationProposalEngine().planLocator(input(declaration,candidate(declaration,"id","row-123")));
        assertEquals(MigrationProposal.Eligibility.MANUAL_ONLY,proposal.eligibility());assertEquals("PARAMETERIZATION_MUST_BE_PRESERVED",proposal.problemCode());assertTrue(proposal.sourceEdits().isEmpty());assertNull(proposal.patchPreview());
    }

    @Test void utf8BomCrLfAndSupplementaryPrefixPreserveExactRange()throws Exception{
        String source="package p;\r\nimport org.openqa.selenium.By;\r\nclass Page { String x=\"😀\"; By save=By.id(\"save\"); }\r\n";
        byte[]body=source.getBytes(StandardCharsets.UTF_8),bytes=new byte[body.length+3];bytes[0]=(byte)0xef;bytes[1]=(byte)0xbb;bytes[2]=(byte)0xbf;System.arraycopy(body,0,bytes,3,body.length);
        Path file=write("src/p/Page.java",bytes);var declaration=projection().declarations().get(0);var proposal=new MigrationProposalEngine().planLocator(input(declaration,candidate(declaration,"name","save-button")));
        assertEquals(MigrationProposal.Eligibility.READY_FOR_REVIEW,proposal.eligibility());assertEquals(MigrationSourcePrecondition.Encoding.UTF8_BOM,proposal.sourcePreconditions().get(0).encoding());assertEquals(MigrationSourcePrecondition.Newline.CRLF,proposal.sourcePreconditions().get(0).newline());assertArrayEquals(bytes,Files.readAllBytes(file));
    }

    @Test void mixedNewlineAndStaleFileAreBlocked()throws Exception{
        Path file=write("src/p/Page.java","package p;\nimport org.openqa.selenium.By;\r\nclass Page { By save=By.id(\"save\"); }\n".getBytes(StandardCharsets.UTF_8));var d=projection().declarations().get(0);var c=candidate(d,"name","save");assertEquals(MigrationProposal.Eligibility.MANUAL_ONLY,new MigrationProposalEngine().planLocator(input(d,c)).eligibility());
        Files.writeString(file,"package p; class Changed {}",StandardCharsets.UTF_8);assertEquals(MigrationProposal.Eligibility.BLOCKED_STALE_INPUT,new MigrationProposalEngine().planLocator(input(d,c)).eligibility());
    }

    @Test void staticImportStrategyChangeNeedsManualImportRewrite()throws Exception{
        write("src/p/Page.java","package p; import static org.openqa.selenium.By.id; class Page { Object save=id(\"save\"); }".getBytes(StandardCharsets.UTF_8));var d=projection().declarations().get(0);var p=new MigrationProposalEngine().planLocator(input(d,candidate(d,"css selector","#save")));assertEquals(MigrationProposal.Eligibility.MANUAL_ONLY,p.eligibility());assertEquals("STATIC_IMPORT_REWRITE_UNSUPPORTED",p.problemCode());
    }

    @Test void conflictsAndLedgerInvalidateChangedInputs()throws Exception{
        write("src/p/Page.java","package p; import org.openqa.selenium.By; class Page { By save=By.id(\"save\"); }".getBytes(StandardCharsets.UTF_8));var d=projection().declarations().get(0);var a=new MigrationProposalEngine().planLocator(input(d,candidate(d,"name","save")));var b=new MigrationProposalEngine().planLocator(input(d,candidate(d,"css selector","#save")));var set=MigrationProposalSet.create("checkpoint",null,List.of(a,b),List.of(),MigrationProposalSet.Completeness.COMPLETE,List.of());assertEquals("SAME_DECLARATION_DIFFERENT_SEMANTICS",set.conflicts().get(0).code());var decision=MigrationDecisionLedger.decide(a,"checkpoint",MigrationDecisionLedger.Decision.APPROVE_FOR_S11,"host-review",null,null);var ledger=MigrationDecisionLedger.create(List.of(decision));assertTrue(ledger.validateApproval(a,"checkpoint",a.evidenceRefs()).valid());assertFalse(ledger.validateApproval(a,"changed",a.evidenceRefs()).valid());
    }

    @Test void javaLiteralEscapesOnlyJavaSyntax(){assertEquals("\"a\\\"b\\\\c\\r\\n\\t😀\"",MigrationProposalEngine.javaLiteral("a\"b\\c\r\n\t😀"));}

    private TrustedSelectorSourceProjection.Snapshot projection(){return TrustedSelectorSourceProjection.scan(temporary,List.of(Path.of("src")),List.of());}
    private MigrationProposalEngine.Input input(TrustedSelectorSourceProjection.Declaration d,TrustedCandidateMaterial c){return new MigrationProposalEngine.Input(temporary,d,null,c,List.of(),"migration-repository-binding-v1:sha256:"+"1".repeat(64),"migration-worktree-binding-v1:sha256:"+"2".repeat(64),"checkpoint");}
    private TrustedCandidateMaterial candidate(TrustedSelectorSourceProjection.Declaration d,String strategy,String value){return new TrustedCandidateMaterial(d.declarationRef(),"candidate",strategy,value,"trusted-analysis",TrustedCandidateMaterial.ValidationState.VERIFIED_IN_SCOPE,TrustedCandidateMaterial.TargetComparison.SAME_TARGET,TrustedCandidateMaterial.Intent.FIND_ONE,1,true,false,false,List.of(),"sha256:"+"a".repeat(64));}
    private Path write(String logical,byte[]bytes)throws Exception{Path file=temporary.resolve(logical);Files.createDirectories(file.getParent());Files.write(file,bytes);return file;}
}
