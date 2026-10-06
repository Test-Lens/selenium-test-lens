package io.github.testlens.application.tooling.json;

import io.github.testlens.application.model.ApplicationModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Deterministic schema-v1 ApplicationModel codec. Unknown fields are rejected except {@code x-*} extensions. @since 0.5.0 */
public final class ApplicationModelJson {
    public byte[] write(ApplicationModel model) { return StrictJson.write(model); }

    public ApplicationModel read(byte[] document) {
        try {
            Map<String,Object> root = StrictJson.readObject(document);
            fields(root, Set.of("schemaVersion","applicationId","applicationName","generatorVersion","generatedAt","pages",
                    "sharedComponents","transitions","coverage","limitations","provenance"), "ApplicationModel");
            return new ApplicationModel(integer(root,"schemaVersion"), string(root,"applicationId"), string(root,"applicationName"),
                    string(root,"generatorVersion"), Instant.parse(string(root,"generatedAt")), list(root,"pages",this::page),
                    list(root,"sharedComponents",this::component), list(root,"transitions",this::transition),
                    coverage(object(root,"coverage")), list(root,"limitations",this::limitation), provenance(object(root,"provenance")));
        } catch (StrictJson.JsonFormatException failure) {
            throw failure;
        } catch (IllegalArgumentException failure) {
            throw new StrictJson.JsonFormatException("ApplicationModel validation failed", failure);
        }
    }

    private ApplicationModel.PageModel page(Map<String,Object> value) {
        fields(value,Set.of("pageId","canonicalName","identity","titles","fingerprints","states","regions","elements","sharedComponentRefs","transitions","provenance","limitations"),"PageModel");
        return new ApplicationModel.PageModel(string(value,"pageId"),string(value,"canonicalName"),identity(object(value,"identity")),
                strings(value,"titles"),strings(value,"fingerprints"),list(value,"states",this::state),list(value,"regions",this::region),
                list(value,"elements",this::element),strings(value,"sharedComponentRefs"),list(value,"transitions",this::transition),
                provenance(object(value,"provenance")),list(value,"limitations",this::limitation));
    }
    private ApplicationModel.PageIdentity identity(Map<String,Object> v){
        fields(v,Set.of("normalizedUrlPattern","route","titlePattern","landmarkElementIds","structuralFingerprint","source"),"PageIdentity");
        return new ApplicationModel.PageIdentity(string(v,"normalizedUrlPattern"),nullable(v,"route"),nullable(v,"titlePattern"),strings(v,"landmarkElementIds"),nullable(v,"structuralFingerprint"),enumeration(v,"source",ApplicationModel.EvidenceSource.class));
    }
    private ApplicationModel.PageState state(Map<String,Object> v){fields(v,Set.of("stateId","semanticName","distinguishingSignals","elementIds","provenance"),"PageState");return new ApplicationModel.PageState(string(v,"stateId"),string(v,"semanticName"),strings(v,"distinguishingSignals"),strings(v,"elementIds"),provenance(object(v,"provenance")));}
    private ApplicationModel.Region region(Map<String,Object> v){fields(v,Set.of("regionId","semanticName","role","fingerprint","provenance"),"Region");return new ApplicationModel.Region(string(v,"regionId"),string(v,"semanticName"),nullable(v,"role"),string(v,"fingerprint"),provenance(object(v,"provenance")));}
    private ApplicationModel.ElementModel element(Map<String,Object> v){
        fields(v,Set.of("elementId","semanticName","type","semanticRole","label","accessibleName","actions","preferredSelector","alternativeSelectors","selectorQuality","regionId","fingerprint","provenance","limitations"),"ElementModel");
        return new ApplicationModel.ElementModel(string(v,"elementId"),string(v,"semanticName"),enumeration(v,"type",ApplicationModel.ElementType.class),nullable(v,"semanticRole"),nullable(v,"label"),nullable(v,"accessibleName"),
                enums(v,"actions",ApplicationModel.Action.class),nullableObject(v,"preferredSelector",this::selector),list(v,"alternativeSelectors",this::selector),enumeration(v,"selectorQuality",ApplicationModel.SelectorQuality.class),nullable(v,"regionId"),string(v,"fingerprint"),provenance(object(v,"provenance")),list(v,"limitations",this::limitation));
    }
    private ApplicationModel.SelectorProjection selector(Map<String,Object> v){fields(v,Set.of("strategy","value","candidateId","validation","sameTarget","unique","stability","rankingPosition","reasons","limitations","source"),"SelectorProjection");return new ApplicationModel.SelectorProjection(string(v,"strategy"),string(v,"value"),string(v,"candidateId"),nullable(v,"validation"),nullable(v,"sameTarget"),bool(v,"unique"),strings(v,"stability"),integer(v,"rankingPosition"),strings(v,"reasons"),strings(v,"limitations"),enumeration(v,"source",ApplicationModel.EvidenceSource.class));}
    private ApplicationModel.SharedComponent component(Map<String,Object> v){fields(v,Set.of("componentId","canonicalName","pageIds","elementIds","fingerprints","source","provenance"),"SharedComponent");return new ApplicationModel.SharedComponent(string(v,"componentId"),string(v,"canonicalName"),strings(v,"pageIds"),strings(v,"elementIds"),strings(v,"fingerprints"),enumeration(v,"source",ApplicationModel.EvidenceSource.class),provenance(object(v,"provenance")));}
    private ApplicationModel.Transition transition(Map<String,Object> v){fields(v,Set.of("transitionId","sourcePageId","sourceStateId","elementId","action","targetPageId","targetStateId","confidence","provenance"),"Transition");return new ApplicationModel.Transition(string(v,"transitionId"),string(v,"sourcePageId"),nullable(v,"sourceStateId"),nullable(v,"elementId"),enumeration(v,"action",ApplicationModel.Action.class),nullable(v,"targetPageId"),nullable(v,"targetStateId"),enumeration(v,"confidence",ApplicationModel.TransitionConfidence.class),provenance(object(v,"provenance")));}
    private ApplicationModel.Provenance provenance(Map<String,Object> v){fields(v,Set.of("source","observationId","contextDescriptor","evidence","attributes"),"Provenance");return new ApplicationModel.Provenance(enumeration(v,"source",ApplicationModel.EvidenceSource.class),nullable(v,"observationId"),nullable(v,"contextDescriptor"),strings(v,"evidence"),stringMap(v,"attributes"));}
    private ApplicationModel.Coverage coverage(Map<String,Object> v){fields(v,Set.of("completeness","observations","pages","states","discoveredNodes","analyzedElements","skippedElements","selectorAnalyses","transitions","truncated"),"Coverage");return new ApplicationModel.Coverage(enumeration(v,"completeness",ApplicationModel.Completeness.class),integer(v,"observations"),integer(v,"pages"),integer(v,"states"),integer(v,"discoveredNodes"),integer(v,"analyzedElements"),integer(v,"skippedElements"),integer(v,"selectorAnalyses"),integer(v,"transitions"),bool(v,"truncated"));}
    private ApplicationModel.Limitation limitation(Map<String,Object> v){fields(v,Set.of("code","detail"),"Limitation");return new ApplicationModel.Limitation(string(v,"code"),string(v,"detail"));}

    private static void fields(Map<String,Object> value,Set<String> known,String where){for(String field:value.keySet())if(!known.contains(field)&&!field.startsWith("x-"))throw new StrictJson.JsonFormatException(where+" contains unknown field: "+field);}
    private static String string(Map<String,Object> v,String n){Object x=v.get(n);if(!(x instanceof String s)||s.isBlank())throw new StrictJson.JsonFormatException(n+" must be a non-empty string");return s;}
    private static String nullable(Map<String,Object>v,String n){Object x=v.get(n);if(x==null)return null;if(!(x instanceof String s))throw new StrictJson.JsonFormatException(n+" must be a string or null");return s;}
    private static int integer(Map<String,Object>v,String n){Object x=v.get(n);if(!(x instanceof Number number))throw new StrictJson.JsonFormatException(n+" must be an integer");long value=number.longValue();if(number.doubleValue()!=value||value<Integer.MIN_VALUE||value>Integer.MAX_VALUE)throw new StrictJson.JsonFormatException(n+" is not an in-range integer");return (int)value;}
    private static boolean bool(Map<String,Object>v,String n){Object x=v.get(n);if(!(x instanceof Boolean b))throw new StrictJson.JsonFormatException(n+" must be boolean");return b;}
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Map<String,Object>v,String n){Object x=v.get(n);if(!(x instanceof Map<?,?>))throw new StrictJson.JsonFormatException(n+" must be an object");return (Map<String,Object>)x;}
    @SuppressWarnings("unchecked") private static <T>T nullableObject(Map<String,Object>v,String n,Function<Map<String,Object>,T>mapper){Object x=v.get(n);if(x==null)return null;if(!(x instanceof Map<?,?>))throw new StrictJson.JsonFormatException(n+" must be object or null");return mapper.apply((Map<String,Object>)x);}
    @SuppressWarnings("unchecked") private static <T>List<T> list(Map<String,Object>v,String n,Function<Map<String,Object>,T>mapper){Object x=v.get(n);if(!(x instanceof List<?> values))throw new StrictJson.JsonFormatException(n+" must be an array");List<T>out=new ArrayList<>();for(Object item:values){if(!(item instanceof Map<?,?>))throw new StrictJson.JsonFormatException(n+" entries must be objects");out.add(mapper.apply((Map<String,Object>)item));}return List.copyOf(out);}
    private static List<String>strings(Map<String,Object>v,String n){Object x=v.get(n);if(!(x instanceof List<?>values))throw new StrictJson.JsonFormatException(n+" must be an array");List<String>out=new ArrayList<>();for(Object item:values){if(!(item instanceof String s))throw new StrictJson.JsonFormatException(n+" entries must be strings");out.add(s);}return List.copyOf(out);}
    private static <E extends Enum<E>>E enumeration(Map<String,Object>v,String n,Class<E>type){try{return Enum.valueOf(type,string(v,n));}catch(IllegalArgumentException failure){throw new StrictJson.JsonFormatException(n+" has unsupported value",failure);}}
    private static <E extends Enum<E>>List<E>enums(Map<String,Object>v,String n,Class<E>type){List<E>out=new ArrayList<>();for(String item:strings(v,n))try{out.add(Enum.valueOf(type,item));}catch(IllegalArgumentException failure){throw new StrictJson.JsonFormatException(n+" has unsupported value",failure);}return List.copyOf(out);}
    private static Map<String,String>stringMap(Map<String,Object>v,String n){Map<String,Object>raw=object(v,n);var out=new java.util.TreeMap<String,String>();raw.forEach((key,value)->{if(!(value instanceof String s))throw new StrictJson.JsonFormatException(n+" values must be strings");out.put(key,s);});return Map.copyOf(out);}
}
