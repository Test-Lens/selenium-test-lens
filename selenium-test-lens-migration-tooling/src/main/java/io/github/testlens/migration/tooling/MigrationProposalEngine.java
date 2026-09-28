package io.github.testlens.migration.tooling;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.testlens.selector.tooling.TrustedSelectorSourceProjection;
import io.github.testlens.selector.tooling.TrustedSelectorUseGraph;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Source-safe S10 proposal planner. It reads source but never writes it. */
public final class MigrationProposalEngine {
    public static final long MAX_SOURCE_BYTES=64L*1024*1024,MAX_DIFF_BYTES=8L*1024*1024;
    private static final Map<String,String> FACTORIES=Map.of("id","id","css selector","cssSelector","name","name",
            "class name","className","tag name","tagName","link text","linkText","partial link text","partialLinkText","xpath","xpath");

    public MigrationProposal planLocator(Input input){
        Objects.requireNonNull(input);var declaration=input.projection();var candidate=input.candidate();
        List<MigrationEvidenceRef> evidence=input.evidenceRefs()==null?List.of():input.evidenceRefs();
        if(!declaration.declarationRef().equals(candidate.declarationRef()))return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_INSUFFICIENT_EVIDENCE,"DECLARATION_CORRELATION_REQUIRED",evidence);
        if(declaration.freshness()!=TrustedSelectorSourceProjection.Freshness.CURRENT)return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_STALE_INPUT,"SOURCE_PROJECTION_NOT_FRESH",evidence);
        if(!FACTORIES.containsKey(candidate.strategy()))return nonPatch(input,MigrationProposal.Eligibility.MANUAL_ONLY,"UNSUPPORTED_LOCATOR_STRATEGY",evidence);
        if(declaration.expressionShape()==TrustedSelectorSourceProjection.ExpressionShape.PARAMETERIZED
                ||declaration.expressionShape()==TrustedSelectorSourceProjection.ExpressionShape.CONCATENATED)
            return nonPatch(input,MigrationProposal.Eligibility.MANUAL_ONLY,"PARAMETERIZATION_MUST_BE_PRESERVED",evidence);
        if(declaration.expressionShape()==TrustedSelectorSourceProjection.ExpressionShape.HELPER_GENERATED
                ||declaration.expressionShape()==TrustedSelectorSourceProjection.ExpressionShape.CUSTOM
                ||declaration.expressionShape()==TrustedSelectorSourceProjection.ExpressionShape.ANNOTATION)
            return nonPatch(input,MigrationProposal.Eligibility.MANUAL_ONLY,"SOURCE_SHAPE_REQUIRES_MANUAL_CHANGE",evidence);
        if(candidate.validationState()!=TrustedCandidateMaterial.ValidationState.VERIFIED_IN_SCOPE
                &&candidate.validationState()!=TrustedCandidateMaterial.ValidationState.VALID_FOR_INTENT)
            return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_INSUFFICIENT_EVIDENCE,"LIVE_VALIDATION_REQUIRED",evidence);
        if(candidate.targetComparison()!=TrustedCandidateMaterial.TargetComparison.SAME_TARGET||!candidate.contextCompatible()
                ||candidate.policyConflict()||candidate.policyEvidenceConflict())
            return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_INSUFFICIENT_EVIDENCE,"CANDIDATE_SEMANTICS_NOT_AUTHORIZED",evidence);
        if(candidate.intent()==TrustedCandidateMaterial.Intent.FIND_ONE&&candidate.matchCount()!=1)
            return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_INSUFFICIENT_EVIDENCE,"CARDINALITY_MISMATCH",evidence);
        if(Objects.equals(declaration.locatorStrategy(),candidate.strategy())&&Objects.equals(declaration.canonicalScalarValue(),candidate.canonicalValue()))
            return nonPatch(input,MigrationProposal.Eligibility.NO_CHANGE_RECOMMENDED,"LOCATOR_ALREADY_DESIRED",evidence);
        try{return exactProposal(input,evidence);}catch(Stale stale){return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_STALE_INPUT,stale.getMessage(),evidence);}catch(Manual manual){return nonPatch(input,MigrationProposal.Eligibility.MANUAL_ONLY,manual.getMessage(),evidence);}catch(IOException failure){return nonPatch(input,MigrationProposal.Eligibility.BLOCKED_STALE_INPUT,"SOURCE_READ_FAILED",evidence);}
    }

    /** Computes the exact local-sensitive preview bytes separately from proposal JSON metadata. */
    public PlanningResult planLocatorWithPreview(Input input){MigrationProposal proposal=planLocator(input);if(proposal.patchPreview()==null)return new PlanningResult(proposal,new byte[0]);try{Path root=input.projectRoot().toRealPath(),file=safeSource(root,input.projection().logicalPath());byte[]bytes=Files.readAllBytes(file);if(!proposal.sourcePreconditions().get(0).fileSha256().equals("sha256:"+hexSha(bytes)))throw new IllegalStateException("source changed during preview");boolean bom=bytes.length>=3&&(bytes[0]&255)==0xef&&(bytes[1]&255)==0xbb&&(bytes[2]&255)==0xbf;String source=decode(bytes,bom?3:0);String proposed=apply(source,proposal.sourceEdits());MigrationSourceEdit edit=proposal.sourceEdits().get(0);byte[]patch=UnifiedDiff.exact(edit.logicalPath(),source,proposed,edit,proposal.sourcePreconditions().get(0).newline()).getBytes(StandardCharsets.UTF_8);if(!proposal.patchPreview().digest().equals("sha256:"+hexSha(patch)))throw new IllegalStateException("patch preview digest changed");return new PlanningResult(proposal,patch);}catch(IOException e){throw new IllegalStateException("preview source unavailable",e);}}

    private MigrationProposal exactProposal(Input input,List<MigrationEvidenceRef>evidence)throws IOException{
        Path root=input.projectRoot().toRealPath(),file=safeSource(root,input.projection().logicalPath());
        if(Files.isSymbolicLink(file))throw new Manual("SYMLINK_SOURCE_UNSUPPORTED");
        long size=Files.size(file);if(size>MAX_SOURCE_BYTES)throw new Manual("SOURCE_FILE_LIMIT_EXCEEDED");
        byte[]bytes=Files.readAllBytes(file);String hash="sha256:"+hexSha(bytes);
        if(!hash.equals(input.projection().fileSha256()))throw new Stale("SOURCE_FILE_DIGEST_CHANGED");
        boolean bom=bytes.length>=3&&(bytes[0]&255)==0xef&&(bytes[1]&255)==0xbb&&(bytes[2]&255)==0xbf;
        String source=decode(bytes,bom?3:0);MigrationSourcePrecondition.Newline newline=newline(source);
        var range=input.projection().range();if(range.endOffsetExclusive()>source.length())throw new Stale("SOURCE_RANGE_CHANGED");
        String original=source.substring(range.startOffset(),range.endOffsetExclusive());
        CompilationUnit unit=parse(source);MethodCallExpr call=exactCall(unit,range.startOffset(),range.endOffsetExclusive(),source);
        if(call.getArguments().size()!=1)throw new Stale("EXPECTED_BY_CALL_NOT_FOUND");
        String oldFactory=call.getNameAsString(),newFactory=FACTORIES.get(input.candidate().strategy());
        if(!FACTORIES.containsValue(oldFactory))throw new Stale("EXPECTED_BY_FACTORY_NOT_FOUND");
        String oldValue=call.getArgument(0)instanceof StringLiteralExpr literal?literal.asString():null;
        if(oldValue==null||!Objects.equals(oldValue,input.projection().canonicalScalarValue()))throw new Stale("SOURCE_SEMANTICS_CHANGED");
        boolean scoped=call.getScope().isPresent();
        if(!scoped&&!oldFactory.equals(newFactory)&&!hasStaticImport(unit,newFactory))throw new Manual("STATIC_IMPORT_REWRITE_UNSUPPORTED");
        String prefix=scoped?call.getScope().orElseThrow()+".":"";String replacement=prefix+newFactory+"("+javaLiteral(input.candidate().canonicalValue())+")";
        String beforeSemantic=semantic(input.projection().locatorStrategy(),oldValue),afterSemantic=semantic(input.candidate().strategy(),input.candidate().canonicalValue());
        String originalDigest="migration-source-construct-v1:sha256:"+MigrationDigests.digest("migration-source-construct-v1",original);
        MigrationSourceEdit edit=new MigrationSourceEdit(input.projection().logicalPath(),range.startOffset(),range.endOffsetExclusive(),
                originalDigest,replacement,beforeSemantic,afterSemantic);
        String proposed=apply(source,List.of(edit));assertMinimal(source,proposed,List.of(edit));
        byte[]diff=UnifiedDiff.exact(input.projection().logicalPath(),source,proposed,edit,newline).getBytes(StandardCharsets.UTF_8);
        if(diff.length>MAX_DIFF_BYTES)throw new Manual("PATCH_PREVIEW_LIMIT_EXCEEDED");
        String patchDigest="sha256:"+hexSha(diff);
        MigrationSourcePrecondition pre=new MigrationSourcePrecondition(input.projection().logicalPath(),hash,
                bom?MigrationSourcePrecondition.Encoding.UTF8_BOM:MigrationSourcePrecondition.Encoding.UTF8,newline,
                "MethodCallExpr",input.projection().declarationRef(),input.projection().constructIdentityDigest(),
                new MigrationSourcePrecondition.CharacterRange(range.startOffset(),range.endOffsetExclusive()),originalDigest,
                beforeSemantic,afterSemantic,MigrationSourcePrecondition.SymlinkState.REGULAR_FILE,input.repositoryBindingRef(),
                input.worktreeBindingRef(),input.checkpointRef(),afterSemantic);
        List<TrustedSelectorUseGraph.UseSite>uses=input.useGraph()==null?List.of():input.useGraph().forDeclaration(input.projection().declarationRef());
        int total=input.useGraph()==null?0:input.useGraph().countForDeclaration(input.projection().declarationRef());
        List<String>useRefs=uses.stream().map(TrustedSelectorUseGraph.UseSite::useSiteRef).toList();
        List<String>tests=uses.stream().map(TrustedSelectorUseGraph.UseSite::affectedTestRef).filter(Objects::nonNull).distinct().sorted().toList();
        MigrationProposal.UsageCoverage coverage=input.useGraph()==null?MigrationProposal.UsageCoverage.UNKNOWN:
                input.useGraph().coverage().complete()?MigrationProposal.UsageCoverage.COMPLETE:MigrationProposal.UsageCoverage.PARTIAL;
        var blast=new MigrationProposal.BlastRadius(1,1,total,useRefs,tests,total>1,coverage,
                input.useGraph()==null?List.of("USE_GRAPH_NOT_SUPPLIED"):input.useGraph().issues());
        boolean incompleteCoverage=input.useGraph()!=null&&!input.useGraph().coverage().complete();
        MigrationProposal.Eligibility eligibility=incompleteCoverage?MigrationProposal.Eligibility.REVIEW_REQUIRED:MigrationProposal.Eligibility.READY_FOR_REVIEW;
        String problem=incompleteCoverage?"SOURCE_COVERAGE_INCOMPLETE":"VALIDATED_LOCATOR_REPLACEMENT";
        List<String>limitations=new ArrayList<>(input.candidate().limitations());if(incompleteCoverage)limitations.add("SOURCE_COVERAGE_INCOMPLETE");
        List<MigrationProposal.VerificationStep>verify=new ArrayList<>();verify.add(step(MigrationProposal.VerificationKind.COMPILE_AFFECTED_MODULE));verify.add(step(MigrationProposal.VerificationKind.RUN_DIRECTLY_AFFECTED_TEST));
        if(total>1)verify.add(step(MigrationProposal.VerificationKind.RUN_KNOWN_USE_SITE_TESTS));verify.add(step(MigrationProposal.VerificationKind.RUN_HEADED));verify.add(step(MigrationProposal.VerificationKind.RUN_HEADLESS));verify.add(step(MigrationProposal.VerificationKind.CAPTURE_COMPATIBILITY_MANIFEST));verify.add(step(MigrationProposal.VerificationKind.COMPARE_COMPATIBILITY));
        List<String>targets=List.of(input.projection().logicalPath()),decls=List.of(input.projection().declarationRef());
        String id=MigrationProposal.id(MigrationProposal.Category.LOCATOR,eligibility,problem,evidence,targets,decls,beforeSemantic,afterSemantic,List.of(pre),List.of());
        MigrationProposal.PatchPreview preview=new MigrationProposal.PatchPreview("patches/"+id.replace(':','-')+".patch",patchDigest,diff.length);
        return new MigrationProposal(1,1,id,MigrationProposal.Category.LOCATOR,eligibility,
                problem,evidence,complete(evidence),MigrationProposal.CausalState.CONFIRMED,
                incompleteCoverage?MigrationProposal.Confidence.MEDIUM:MigrationProposal.Confidence.HIGH,targets,decls,useRefs,beforeSemantic,afterSemantic,List.of(pre),List.of(edit),preview,
                blast,verify,new MigrationProposal.RollbackPlan("REVERSE_EXACT_EDIT",afterSemantic,beforeSemantic),limitations,List.of(),List.of(),afterSemantic);
    }

    public MigrationProposal configurationProposal(String problemCode,String current,String proposed,List<MigrationEvidenceRef> evidence){
        var blast=new MigrationProposal.BlastRadius(0,0,0,List.of(),List.of(),false,MigrationProposal.UsageCoverage.UNKNOWN,List.of());
        var verify=List.of(step(MigrationProposal.VerificationKind.CAPTURE_COMPATIBILITY_MANIFEST),step(MigrationProposal.VerificationKind.COMPARE_COMPATIBILITY));
        String id=MigrationProposal.id(MigrationProposal.Category.CONFIGURATION,MigrationProposal.Eligibility.REVIEW_REQUIRED,problemCode,evidence,List.of(),List.of(),current,proposed,List.of(),List.of());
        return new MigrationProposal(1,1,id,MigrationProposal.Category.CONFIGURATION,MigrationProposal.Eligibility.REVIEW_REQUIRED,problemCode,evidence,complete(evidence),MigrationProposal.CausalState.HYPOTHESIS,MigrationProposal.Confidence.MEDIUM,List.of(),List.of(),List.of(),current,proposed,List.of(),List.of(),null,blast,verify,new MigrationProposal.RollbackPlan("RESTORE_CONFIGURATION",null,current),List.of(),List.of(),List.of(),proposed);
    }
    public MigrationProposal advisoryProposal(MigrationProposal.Category category,MigrationProposal.Eligibility eligibility,
                                               String problemCode,String current,String proposed,List<MigrationEvidenceRef>evidence,
                                               MigrationProposal.CausalState causalState,List<String>limitations){
        var blast=new MigrationProposal.BlastRadius(0,0,0,List.of(),List.of(),false,MigrationProposal.UsageCoverage.UNKNOWN,List.of());
        var verify=List.of(step(MigrationProposal.VerificationKind.MANUAL_REVIEW));
        String id=MigrationProposal.id(category,eligibility,problemCode,evidence,List.of(),List.of(),current,proposed,List.of(),List.of());
        return new MigrationProposal(1,1,id,category,eligibility,problemCode,evidence,complete(evidence),causalState,
                MigrationProposal.Confidence.UNKNOWN,List.of(),List.of(),List.of(),current,proposed,List.of(),List.of(),null,
                blast,verify,new MigrationProposal.RollbackPlan("NONE",null,null),limitations,List.of(),List.of(),proposed);
    }
    private MigrationProposal nonPatch(Input i,MigrationProposal.Eligibility eligibility,String problem,List<MigrationEvidenceRef>evidence){
        var blast=new MigrationProposal.BlastRadius(0,0,0,List.of(),List.of(),false,MigrationProposal.UsageCoverage.UNKNOWN,List.of());
        List<String>targets=List.of(i.projection().logicalPath()),decls=List.of(i.projection().declarationRef());String before=semantic(i.projection().locatorStrategy(),i.projection().canonicalScalarValue()),after=semantic(i.candidate().strategy(),i.candidate().canonicalValue());
        String id=MigrationProposal.id(MigrationProposal.Category.LOCATOR,eligibility,problem,evidence,targets,decls,before,after,List.of(),List.of());
        return new MigrationProposal(1,1,id,MigrationProposal.Category.LOCATOR,eligibility,problem,evidence,complete(evidence),MigrationProposal.CausalState.UNKNOWN,MigrationProposal.Confidence.UNKNOWN,targets,decls,List.of(),before,after,List.of(),List.of(),null,blast,List.of(step(MigrationProposal.VerificationKind.MANUAL_REVIEW)),new MigrationProposal.RollbackPlan("NONE",null,null),List.of(problem),List.of(),List.of(),after);
    }
    private static MigrationProposal.VerificationStep step(MigrationProposal.VerificationKind k){return new MigrationProposal.VerificationStep(k,MigrationProposal.SideEffect.UNKNOWN,null);}
    private static MigrationProposal.EvidenceCompleteness complete(List<MigrationEvidenceRef>r){return r.isEmpty()?MigrationProposal.EvidenceCompleteness.UNKNOWN:r.stream().allMatch(x->x.completeness()==MigrationEvidenceRef.Completeness.COMPLETE)?MigrationProposal.EvidenceCompleteness.COMPLETE:MigrationProposal.EvidenceCompleteness.PARTIAL;}
    private static Path safeSource(Path root,String logical)throws IOException{if(logical==null||logical.startsWith("/")||logical.contains(":")||logical.contains("..")||logical.equals(".git")||logical.startsWith(".git/"))throw new IllegalArgumentException("unsafe source path");Path file=root.resolve(logical.replace('/',java.io.File.separatorChar)).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||!file.toRealPath().startsWith(root))throw new IllegalArgumentException("source path escapes project");return file;}
    private static String decode(byte[]b,int offset){try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b,offset,b.length-offset)).toString();}catch(CharacterCodingException e){throw new Manual("INVALID_UTF8");}}
    private static MigrationSourcePrecondition.Newline newline(String s){boolean lf=false,crlf=false;for(int i=0;i<s.length();i++)if(s.charAt(i)=='\n'){if(i>0&&s.charAt(i-1)=='\r')crlf=true;else lf=true;}if(lf&&crlf)throw new Manual("MIXED_NEWLINES_UNSUPPORTED");return crlf?MigrationSourcePrecondition.Newline.CRLF:MigrationSourcePrecondition.Newline.LF;}
    private static CompilationUnit parse(String s){var result=new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)).parse(s);if(result.getResult().isEmpty()||!result.isSuccessful())throw new Stale("CURRENT_SOURCE_PARSE_FAILED");return result.getResult().orElseThrow();}
    private static MethodCallExpr exactCall(CompilationUnit unit,int start,int end,String source){return unit.findAll(MethodCallExpr.class).stream().filter(n->n.getRange().isPresent()).filter(n->{var r=n.getRange().orElseThrow();return offset(source,r.begin)==start&&Math.min(source.length(),offset(source,r.end)+1)==end;}).findFirst().orElseThrow(()->new Stale("EXPECTED_SOURCE_NODE_NOT_FOUND"));}
    private static int offset(String source,com.github.javaparser.Position p){int line=1,index=0;while(line<p.line&&index<source.length()){char c=source.charAt(index++);if(c=='\r'){if(index<source.length()&&source.charAt(index)=='\n')index++;line++;}else if(c=='\n')line++;}return Math.min(source.length(),index+p.column-1);}
    private static boolean hasStaticImport(CompilationUnit unit,String name){return unit.getImports().stream().anyMatch(i->i.isStatic()&&(i.getNameAsString().equals("org.openqa.selenium.By."+name)||(i.isAsterisk()&&i.getNameAsString().equals("org.openqa.selenium.By"))));}
    public static String javaLiteral(String value){StringBuilder out=new StringBuilder("\"");value.codePoints().forEach(cp->{switch(cp){case'\"'->out.append("\\\"");case'\\'->out.append("\\\\");case'\r'->out.append("\\r");case'\n'->out.append("\\n");case'\t'->out.append("\\t");case'\b'->out.append("\\b");case'\f'->out.append("\\f");default->{if(cp<0x20||cp==0x7f)out.append(String.format("\\u%04x",cp));else out.appendCodePoint(cp);}}});return out.append('"').toString();}
    private static String apply(String source,List<MigrationSourceEdit>edits){List<MigrationSourceEdit>sorted=edits.stream().sorted(Comparator.comparingInt(MigrationSourceEdit::startUtf16).reversed()).toList();int last=source.length();StringBuilder out=new StringBuilder(source);for(var e:sorted){if(e.endUtf16Exclusive()>last)throw new IllegalArgumentException("overlapping source edits");out.replace(e.startUtf16(),e.endUtf16Exclusive(),e.replacementText());last=e.startUtf16();}return out.toString();}
    private static void assertMinimal(String before,String after,List<MigrationSourceEdit>edits){String reconstructed=apply(before,edits);if(!reconstructed.equals(after))throw new IllegalStateException("unexpected preview bytes outside approved edits");}
    private static String semantic(String strategy,String value){return"locator-semantic-v1:sha256:"+MigrationDigests.digest("locator-semantic-v1",strategy==null?"":strategy,value==null?"":value);}
    private static String hexSha(byte[]b){return java.util.HexFormat.of().formatHex(MigrationDigests.sha256().digest(b));}

    public record Input(Path projectRoot,TrustedSelectorSourceProjection.Declaration projection,TrustedSelectorUseGraph.Graph useGraph,
                        TrustedCandidateMaterial candidate,List<MigrationEvidenceRef>evidenceRefs,String repositoryBindingRef,
                        String worktreeBindingRef,String checkpointRef){public Input{Objects.requireNonNull(projectRoot);Objects.requireNonNull(projection);Objects.requireNonNull(candidate);}}
    public record PlanningResult(MigrationProposal proposal,byte[]exactPatchBytes){public PlanningResult{Objects.requireNonNull(proposal);exactPatchBytes=exactPatchBytes.clone();}@Override public byte[]exactPatchBytes(){return exactPatchBytes.clone();}}
    private static final class Stale extends RuntimeException{Stale(String m){super(m);}}
    private static final class Manual extends RuntimeException{Manual(String m){super(m);}}

    static final class UnifiedDiff{
        private UnifiedDiff(){}
        static String exact(String path,String before,String after,MigrationSourceEdit edit,MigrationSourcePrecondition.Newline newline){
            String sep=newline==MigrationSourcePrecondition.Newline.CRLF?"\r\n":"\n";LinePos old=line(before,edit.startUtf16(),edit.endUtf16Exclusive(),sep),neu=line(after,edit.startUtf16(),edit.startUtf16()+edit.replacementText().length(),sep);int context=3;
            List<String>a=lines(before,sep),b=lines(after,sep);int aStart=Math.max(0,old.index-context),bStart=Math.max(0,neu.index-context);int aEnd=Math.min(a.size(),old.endIndex+context+1),bEnd=Math.min(b.size(),neu.endIndex+context+1);
            StringBuilder d=new StringBuilder("--- a/").append(path).append('\n').append("+++ b/").append(path).append('\n').append("@@ -").append(aStart+1).append(',').append(aEnd-aStart).append(" +").append(bStart+1).append(',').append(bEnd-bStart).append(" @@\n");
            for(int i=aStart;i<old.index;i++)d.append(' ').append(a.get(i)).append('\n');for(int i=old.index;i<=old.endIndex&&i<a.size();i++)d.append('-').append(a.get(i)).append('\n');for(int i=neu.index;i<=neu.endIndex&&i<b.size();i++)d.append('+').append(b.get(i)).append('\n');for(int i=old.endIndex+1;i<aEnd;i++)d.append(' ').append(a.get(i)).append('\n');return d.toString();
        }
        private static List<String>lines(String s,String sep){return List.of(s.split(java.util.regex.Pattern.quote(sep),-1));}
        private static LinePos line(String s,int start,int endOffset,String sep){int index=0,end=0,pos=0;while((pos=s.indexOf(sep,pos))>=0&&pos<start){index++;pos+=sep.length();}end=index;int scan=pos;while((scan=s.indexOf(sep,scan))>=0&&scan<endOffset){end++;scan+=sep.length();}return new LinePos(index,end);}
        private record LinePos(int index,int endIndex){}
    }
}
