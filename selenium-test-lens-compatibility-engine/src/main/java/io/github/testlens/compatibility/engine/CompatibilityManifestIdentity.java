package io.github.testlens.compatibility.engine;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Canonical semantic identity shared by construction and JSON verification. */
public final class CompatibilityManifestIdentity {
    private CompatibilityManifestIdentity() {}
    public static String id(CompatibilityRunManifest r) {
        List<String> parts=new ArrayList<>();
        add(parts,r.schemaVersion());add(parts,r.manifestAlgorithmVersion());add(parts,r.testIdentity());add(parts,r.execution());
        add(parts,r.browser());add(parts,r.display());add(parts,r.context());add(parts,r.configuration());add(parts,r.attempts());
        add(parts,r.terminalResult());add(parts,r.completeness());add(parts,r.issues());add(parts,r.evidenceDigests());
        return "compatibility-manifest-v1:sha256:"+CompatibilityDigests.digest("compatibility-manifest-v1",parts.toArray(String[]::new));
    }

    private static void add(List<String> parts,Object value){
        if(value==null){parts.add("null");return;}
        if(value instanceof Enum<?> e){parts.add("enum");parts.add(e.getDeclaringClass().getName());parts.add(e.name());return;}
        if(value instanceof CharSequence||value instanceof Number||value instanceof Boolean){parts.add(value.getClass().getName());parts.add(String.valueOf(value));return;}
        if(value instanceof List<?> list){parts.add("list");parts.add(Integer.toString(list.size()));list.forEach(v->add(parts,v));return;}
        if(value instanceof Map<?,?> map){parts.add("map");parts.add(Integer.toString(map.size()));map.entrySet().stream().sorted(Comparator.comparing(e->String.valueOf(e.getKey()))).forEach(e->{add(parts,String.valueOf(e.getKey()));add(parts,e.getValue());});return;}
        if(value.getClass().isRecord()){
            parts.add("record");parts.add(value.getClass().getName());
            for(var component:value.getClass().getRecordComponents())try{parts.add(component.getName());add(parts,component.getAccessor().invoke(value));}
            catch(IllegalAccessException|InvocationTargetException failure){throw new IllegalStateException("Cannot canonicalize manifest record",failure);}
            return;
        }
        throw new IllegalArgumentException("Unsupported canonical manifest value "+value.getClass().getName());
    }
}
