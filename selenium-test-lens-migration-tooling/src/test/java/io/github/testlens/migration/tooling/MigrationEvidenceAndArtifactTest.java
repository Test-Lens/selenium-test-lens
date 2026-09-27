package io.github.testlens.migration.tooling;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationEvidenceAndArtifactTest {
 @Test void comparisonImportIsContentAddressedSanitizedAndRejectsDuplicates(){String json="{\"schemaVersion\":1,\"algorithmVersion\":1,\"analysisMode\":\"COMPARE\",\"findings\":[{\"findingId\":\"f\",\"category\":\"LOCATOR\",\"causalState\":\"CONFIRMED_CAUSE\",\"code\":\"LOCATOR_REGRESSION\",\"reasonCodes\":[\"CONTROLLED\"]}],\"outputLimitations\":[]}";var evidence=new MigrationEvidenceImporter().importComparison(json.getBytes(StandardCharsets.UTF_8),"reports/compare.json");assertEquals(MigrationEvidenceRef.TrustClass.SANITIZED_EVIDENCE,evidence.ref().trustClass());assertEquals("LOCATOR_REGRESSION",evidence.facts().get(0).code());assertThrows(IllegalArgumentException.class,()->new MigrationEvidenceImporter().importComparison(json.replace("\"schemaVersion\":1","\"schemaVersion\":1,\"schemaVersion\":1").getBytes(StandardCharsets.UTF_8),"reports/x.json"));assertThrows(IllegalArgumentException.class,()->new MigrationEvidenceImporter().importComparison(json.replace("CONFIRMED_CAUSE","MADE_UP").getBytes(StandardCharsets.UTF_8),"reports/x.json"));}
 @Test void auditAlternativeIsEvidenceButContainsNoPatchMaterial(){String json="{\"auditSchemaVersion\":1,\"auditAlgorithmVersion\":1,\"overallCompleteness\":\"COMPLETE_FOR_REQUESTED_INPUTS\",\"declarations\":[{\"declarationRef\":\"d\",\"findings\":[{\"code\":\"ALTERNATIVE_AVAILABLE\",\"category\":\"IMPROVEMENT\",\"state\":\"ACTIVE\",\"reasonCodes\":[]}]}],\"outputLimitations\":[]}";var evidence=new MigrationEvidenceImporter().importSelectorAudit(json.getBytes(StandardCharsets.UTF_8),"reports/audit.json");assertEquals("ALTERNATIVE_AVAILABLE",evidence.facts().get(0).code());assertFalse(new String(new MigrationArtifactJson().write(evidence),StandardCharsets.UTF_8).contains("canonicalValue"));}
 @Test void logicalEvidencePathsRejectTraversalAndAbsolutePaths(){assertThrows(IllegalArgumentException.class,()->new MigrationEvidenceRef(MigrationEvidenceRef.Type.SELECTOR_AUDIT,1,1,"sha256:"+"a".repeat(64),"../../evil",MigrationEvidenceRef.TrustClass.SANITIZED_EVIDENCE,MigrationEvidenceRef.Completeness.COMPLETE,"local"));assertThrows(IllegalArgumentException.class,()->new MigrationEvidenceRef(MigrationEvidenceRef.Type.SELECTOR_AUDIT,1,1,"sha256:"+"a".repeat(64),"C:/evil",MigrationEvidenceRef.TrustClass.SANITIZED_EVIDENCE,MigrationEvidenceRef.Completeness.COMPLETE,"local"));}
}
