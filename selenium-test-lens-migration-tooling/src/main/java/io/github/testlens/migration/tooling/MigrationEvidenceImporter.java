package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict, bounded reader for sanitized comparison, static, and Selector Audit V1 JSON. */
public final class MigrationEvidenceImporter {
    private static final Set<String> COMPARISON_TOP = Set.of("schemaVersion","algorithmVersion","engineVersions","analysisMode","comparisonIntent","summary","coverage","comparabilitySummary","configurationDifferences","testComparisons","findings","inconclusiveCases","recommendations","unmatchedBaseline","unmatchedVariant","ambiguousMatches","issues","outputLimitations");
    private static final Set<String> STATIC_TOP = Set.of("schemaVersion","algorithmVersion","analysisMode","coverage","findings","detectorDecisions","issues","limitations","metrics");
    private static final Set<String> AUDIT_TOP = Set.of("auditSchemaVersion","auditAlgorithmVersion","engineVersions","project","generatedFrom","analysisMode","overallCompleteness","summary","coverage","projectIssues","declarations","runtimeOnlySubjects","families","outputLimitations");
    private static final Set<String> COMPATIBILITY_CATEGORIES=Set.of("CONFIGURATION","VIEWPORT_RESPONSIVE","LOCATOR","INTERACTION","SCROLL_TIMING","WINDOWS_TABS","DOWNLOAD_UPLOAD","AUTH_SESSION","APPLICATION_BEHAVIOR","TEST_ASSUMPTION","INFRASTRUCTURE","UNKNOWN");
    private static final Set<String> COMPATIBILITY_CAUSAL=Set.of("OBSERVATION","HYPOTHESIS","CONFIRMED_CAUSE");
    private static final Set<String> AUDIT_CATEGORIES=Set.of("SOURCE","APPEARANCE","POLICY","EVIDENCE","VALIDATION","IMPROVEMENT","STRUCTURAL");
    private static final Set<String> AUDIT_STATES=Set.of("ACTIVE","COVERED_BY_POLICY","CONFLICTED","INFORMATIONAL","INCOMPLETE","UNSUPPORTED");
    private static final Set<String> AUDIT_CODES=Set.of("SOURCE_UNRESOLVED_DECLARATION","SOURCE_PARTIALLY_RESOLVED_DECLARATION","SOURCE_DYNAMIC_DECLARATION","SOURCE_CUSTOM_LOCATOR","SOURCE_UNSUPPORTED_DECLARATION","SOURCE_RESOLUTION_ERROR","APPEARANCE_GENERATED_LOOKING","POLICY_DECLARED_STABLE","POLICY_DECLARED_UNSTABLE","POLICY_POLICY_CONFLICT","POLICY_EVIDENCE_CONFLICT","EVIDENCE_OBSERVED_VARIABLE","EVIDENCE_NOT_AVAILABLE","EVIDENCE_HISTORY_INCOMPLETE","EVIDENCE_COMPARABILITY_UNKNOWN","EVIDENCE_RUNTIME_VALUE_REDACTED","EVIDENCE_AMBIGUOUS_CORRELATION","CURRENT_VERIFIED_IN_SCOPE","CURRENT_VALID_BUT_AMBIGUOUS","CURRENT_WRONG_TARGET","CURRENT_NO_MATCH","CURRENT_INVALID_SELECTOR","CURRENT_NOT_LIVE_VALIDATED","CURRENT_STALE_TARGET","CURRENT_TARGET_REQUIRED","CURRENT_LOCATOR_ALREADY_BEST","ALTERNATIVE_AVAILABLE","ALTERNATIVE_REQUIRES_REVIEW","NO_VALID_ALTERNATIVE","DUPLICATE_EXACT_LOCATOR","RELATED_GENERATED_FAMILY","BROAD_PATTERN_IMPACT","USAGE_SCOPE_UNKNOWN");

    public MigrationEvidence importComparison(byte[] bytes, String logicalRef) {
        Map<String,Object> root = MigrationStrictJson.object(bytes, MigrationStrictJson.MAX_EVIDENCE_BYTES);
        exactFields(root, COMPARISON_TOP); required(root,"schemaVersion","algorithmVersion","analysisMode","findings","outputLimitations");version(root,"schemaVersion","algorithmVersion");allowed(string(root.get("analysisMode")),Set.of("COMPARE"),"analysisMode");
        List<MigrationEvidence.Fact> facts = facts(root.get("findings"), "category", "causalState", "findingId");
        facts.forEach(f->{allowed(f.category(),COMPATIBILITY_CATEGORIES,"category");allowed(f.state(),COMPATIBILITY_CAUSAL,"causalState");});
        List<String> limitations = strings(root.get("outputLimitations"));
        return evidence(bytes,logicalRef,MigrationEvidence.Kind.COMPATIBILITY_COMPARISON,
                MigrationEvidenceRef.Type.COMPATIBILITY_COMPARISON,facts,limitations);
    }
    public MigrationEvidence importStatic(byte[] bytes, String logicalRef) {
        Map<String,Object> root = MigrationStrictJson.object(bytes, MigrationStrictJson.MAX_EVIDENCE_BYTES);
        exactFields(root, STATIC_TOP);required(root,"schemaVersion","algorithmVersion","analysisMode","coverage","findings","limitations"); version(root,"schemaVersion","algorithmVersion");allowed(string(root.get("analysisMode")),Set.of("STATIC"),"analysisMode");
        List<MigrationEvidence.Fact> facts = facts(root.get("findings"), "category", "causalState", "findingId");
        facts.forEach(f->{allowed(f.category(),COMPATIBILITY_CATEGORIES,"category");allowed(f.state(),Set.of("HYPOTHESIS"),"causalState");});
        return evidence(bytes,logicalRef,MigrationEvidence.Kind.STATIC_COMPATIBILITY,
                MigrationEvidenceRef.Type.STATIC_COMPATIBILITY,facts,strings(root.get("limitations")));
    }
    public MigrationEvidence importSelectorAudit(byte[] bytes, String logicalRef) {
        Map<String,Object> root = MigrationStrictJson.object(bytes, MigrationStrictJson.MAX_EVIDENCE_BYTES);
        exactFields(root, AUDIT_TOP);required(root,"auditSchemaVersion","auditAlgorithmVersion","overallCompleteness","declarations","outputLimitations"); version(root,"auditSchemaVersion","auditAlgorithmVersion");allowed(string(root.get("overallCompleteness")),Set.of("COMPLETE_FOR_REQUESTED_INPUTS","PARTIAL","LIMITED"),"overallCompleteness");
        List<MigrationEvidence.Fact> facts = new ArrayList<>();
        for (Object declaration : list(root.get("declarations"))) {
            Map<String,Object> d = object(declaration); String subject = string(d.get("declarationRef"));
            for (Object finding : list(d.get("findings"))) {
                Map<String,Object> f=object(finding);String code=enumValue(f,"code"),category=enumValue(f,"category"),state=enumValue(f,"state");allowed(code,AUDIT_CODES,"code");allowed(category,AUDIT_CATEGORIES,"category");allowed(state,AUDIT_STATES,"state");facts.add(new MigrationEvidence.Fact(code,category,state,subject,strings(f.get("reasonCodes"))));
            }
        }
        return evidence(bytes,logicalRef,MigrationEvidence.Kind.SELECTOR_AUDIT,
                MigrationEvidenceRef.Type.SELECTOR_AUDIT,facts,strings(root.get("outputLimitations")));
    }
    private MigrationEvidence evidence(byte[] bytes,String logical,MigrationEvidence.Kind kind,MigrationEvidenceRef.Type type,
                                       List<MigrationEvidence.Fact> facts,List<String> limitations){
        var completeness=limitations.isEmpty()?MigrationEvidenceRef.Completeness.COMPLETE:MigrationEvidenceRef.Completeness.PARTIAL;
        var ref=new MigrationEvidenceRef(type,1,1,"sha256:"+java.util.HexFormat.of().formatHex(MigrationDigests.sha256().digest(bytes)),logical,
                MigrationEvidenceRef.TrustClass.SANITIZED_EVIDENCE,completeness,"validated-local-import");
        return new MigrationEvidence(ref,kind,facts,limitations);
    }
    private static void version(Map<String,Object> root,String schema,String algorithm){if(number(root.get(schema))!=1||number(root.get(algorithm))!=1)throw new IllegalArgumentException("unsupported evidence version");}
    private static void exactFields(Map<String,Object> root,Set<String> allowed){for(String field:root.keySet())if(!allowed.contains(field)&&!field.startsWith("x-"))throw new IllegalArgumentException("unknown evidence field: "+field);}
    private static void required(Map<String,Object>root,String...fields){for(String field:fields)if(!root.containsKey(field))throw new IllegalArgumentException("missing evidence field: "+field);}
    private static void allowed(String value,Set<String>allowed,String field){if(!allowed.contains(value))throw new IllegalArgumentException("invalid "+field+": "+value);}
    private static List<MigrationEvidence.Fact> facts(Object raw,String category,String state,String subject){List<MigrationEvidence.Fact>out=new ArrayList<>();for(Object item:list(raw)){Map<String,Object>f=object(item);String code=f.containsKey("code")?enumValue(f,"code"):f.containsKey("findingCode")?enumValue(f,"findingCode"):"UNKNOWN";out.add(new MigrationEvidence.Fact(code,nullable(f.get(category)),nullable(f.get(state)),nullable(f.get(subject)),strings(f.get("reasonCodes"))));if(out.size()>MigrationEvidence.MAX_FACTS)throw new IllegalArgumentException("evidence facts bound");}return out;}
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value){if(!(value instanceof Map<?,?>))throw new IllegalArgumentException("object required");return(Map<String,Object>)value;}
    @SuppressWarnings("unchecked") private static List<Object> list(Object value){if(value==null)return List.of();if(!(value instanceof List<?>))throw new IllegalArgumentException("array required");return(List<Object>)value;}
    private static List<String> strings(Object value){List<String>out=new ArrayList<>();for(Object x:list(value)){out.add(string(x));if(out.size()>512)throw new IllegalArgumentException("string list bound");}return List.copyOf(out);}
    private static String enumValue(Map<String,Object> value,String field){String result=string(value.get(field));if(!result.matches("[A-Z][A-Z0-9_]{0,127}"))throw new IllegalArgumentException("invalid enum "+field);return result;}
    private static String string(Object value){if(!(value instanceof String s)||s.length()>8192||s.indexOf('\0')>=0)throw new IllegalArgumentException("string required");return s;}
    private static String nullable(Object value){return value==null?null:string(value);}
    private static long number(Object value){if(!(value instanceof Number number))throw new IllegalArgumentException("number required");return number.longValue();}
}
