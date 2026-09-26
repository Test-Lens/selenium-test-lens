package io.github.testlens.selector.engine;

import java.time.Instant;

/** Explicit trusted metadata for offline cross-run comparison; no test arguments are inspected. */
public record RunDescriptor(String framework,String testClass,String testMethod,String logicalTestKey,
                            String invocationDiscriminator,int attempt,String datasetKey,String testSourceRevision,
                            String systemUnderTestRevision,String environmentKey,Instant observedAt,String chronologyKey,Long runSequence){
    public RunDescriptor{if(attempt<0)throw new IllegalArgumentException("attempt must be nonnegative");}
    public String datasetKeyDigest(){return datasetKey==null?null:"selector-dataset-key-v1:sha256:"+CanonicalDigests.digest("selector-dataset-key-v1",datasetKey);}
    public String environmentKeyDigest(){return environmentKey==null?null:"selector-environment-key-v1:sha256:"+CanonicalDigests.digest("selector-environment-key-v1",environmentKey);}
    public String logicalTestIdentityRef(){return "selector-logical-test-v1:sha256:"+CanonicalDigests.digest("selector-logical-test-v1",n(framework),n(testClass),n(testMethod),n(logicalTestKey));}
    public String runRef(){return "selector-run-v1:sha256:"+CanonicalDigests.digest("selector-run-v1",logicalTestIdentityRef(),n(invocationDiscriminator),Integer.toString(attempt),n(datasetKeyDigest()),n(testSourceRevision),n(systemUnderTestRevision),n(environmentKeyDigest()));}
    public boolean chronologyKnown(){return observedAt!=null||chronologyKey!=null||runSequence!=null;}
    private static String n(String v){return v==null?"":v;}
}
