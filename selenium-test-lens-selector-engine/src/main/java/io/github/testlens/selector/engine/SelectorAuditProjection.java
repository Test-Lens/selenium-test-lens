package io.github.testlens.selector.engine;

import java.util.List;

/** Sanitized JDK-only Audit view for interactive tooling. It contains no raw selector or policy note. */
public record SelectorAuditProjection(int schemaVersion,List<Declaration>declarations,List<String>coverageLimitations){
    public static final int SCHEMA_VERSION=1;
    public SelectorAuditProjection{declarations=List.copyOf(declarations);coverageLimitations=List.copyOf(coverageLimitations);}
    public record Declaration(String declarationRef,Source source,List<Finding>findings,Summary evidence,
                              Summary policy,String recommendation,List<String>familyRefs,List<String>limitations){
        public Declaration{findings=List.copyOf(findings);familyRefs=List.copyOf(familyRefs);limitations=List.copyOf(limitations);}
    }
    public record Source(String logicalPath,int startLine,int startColumn,int endLine,int endColumn){ }
    public record Finding(String severity,String state,String code,List<String>reasonCodes){
        public Finding{reasonCodes=List.copyOf(reasonCodes);}
    }
    public record Summary(String state,List<String>reasonCodes,boolean incomplete){
        public Summary{reasonCodes=List.copyOf(reasonCodes);}
    }
    public Declaration declaration(String declarationRef){
        if(declarationRef==null)return null;
        return declarations.stream().filter(value->declarationRef.equals(value.declarationRef())).findFirst().orElse(null);
    }
}
