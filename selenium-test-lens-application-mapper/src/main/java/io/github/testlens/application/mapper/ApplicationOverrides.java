package io.github.testlens.application.mapper;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Declarative, reviewable identity and naming overrides; never references Selenium object IDs. @since 0.5.0 */
public record ApplicationOverrides(int schemaVersion, Map<String,String> pageNamesByUrlPattern,
                                   Map<String,String> elementNamesByFingerprint,
                                   Set<String> sharedRegionFingerprints, Set<String> deniedUrlPatterns,
                                   Map<String,String> pageIdentityGroupsByObservationFingerprint,
                                   Map<String,String> pageNamesByIdentityGroup) {
    public static final int SCHEMA_VERSION=1;
    public ApplicationOverrides {
        if(schemaVersion!=SCHEMA_VERSION)throw new IllegalArgumentException("Unsupported override schemaVersion: "+schemaVersion);
        pageNamesByUrlPattern=Map.copyOf(new TreeMap<>(pageNamesByUrlPattern==null?Map.of():pageNamesByUrlPattern));
        elementNamesByFingerprint=Map.copyOf(new TreeMap<>(elementNamesByFingerprint==null?Map.of():elementNamesByFingerprint));
        sharedRegionFingerprints=Set.copyOf(new TreeSet<>(sharedRegionFingerprints==null?Set.of():sharedRegionFingerprints));
        deniedUrlPatterns=Set.copyOf(new TreeSet<>(deniedUrlPatterns==null?Set.of():deniedUrlPatterns));
        pageIdentityGroupsByObservationFingerprint=Map.copyOf(new TreeMap<>(pageIdentityGroupsByObservationFingerprint==null?Map.of():pageIdentityGroupsByObservationFingerprint));
        pageNamesByIdentityGroup=Map.copyOf(new TreeMap<>(pageNamesByIdentityGroup==null?Map.of():pageNamesByIdentityGroup));
    }
    public ApplicationOverrides(int schemaVersion,Map<String,String>pageNamesByUrlPattern,Map<String,String>elementNamesByFingerprint,Set<String>sharedRegionFingerprints,Set<String>deniedUrlPatterns){this(schemaVersion,pageNamesByUrlPattern,elementNamesByFingerprint,sharedRegionFingerprints,deniedUrlPatterns,Map.of(),Map.of());}
    public ApplicationOverrides(int schemaVersion,Map<String,String>pageNamesByUrlPattern,Map<String,String>elementNamesByFingerprint,Set<String>sharedRegionFingerprints,Set<String>deniedUrlPatterns,Map<String,String>pageIdentityGroupsByObservationFingerprint){this(schemaVersion,pageNamesByUrlPattern,elementNamesByFingerprint,sharedRegionFingerprints,deniedUrlPatterns,pageIdentityGroupsByObservationFingerprint,Map.of());}
    public static ApplicationOverrides empty(){return new ApplicationOverrides(SCHEMA_VERSION,Map.of(),Map.of(),Set.of(),Set.of(),Map.of(),Map.of());}
}
