package io.github.testlens.compatibility.tooling;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StrictJson {
    static final long MAX_INPUT_BYTES=16L*1024*1024;
    private static final JsonFactory FACTORY=JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxDocumentLength(MAX_INPUT_BYTES)
                    .maxNestingDepth(32).maxStringLength(65_536).maxNameLength(256)
                    .maxNumberLength(64).maxTokenCount(2_000_000).build()).build();
    private StrictJson(){}
    static Map<String,Object> object(byte[] bytes){
        if(bytes==null||bytes.length==0||bytes.length>MAX_INPUT_BYTES)throw new FormatException("JSON input size is invalid");
        try(JsonParser parser=FACTORY.createParser(ObjectReadContext.empty(),new ByteArrayInputStream(bytes))){
            if(parser.nextToken()!=JsonToken.START_OBJECT)throw new FormatException("JSON root must be object");
            @SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)read(parser,0);
            if(parser.nextToken()!=null)throw new FormatException("Trailing JSON content");
            return result;
        }catch(IOException e){throw new FormatException("Malformed JSON",e);}
    }
    private static Object read(JsonParser p,int depth)throws IOException{
        if(depth>32)throw new FormatException("JSON nesting exceeds limit");
        return switch(p.currentToken()){
            case START_OBJECT->{Map<String,Object> map=new LinkedHashMap<>();while(p.nextToken()!=JsonToken.END_OBJECT){String name=p.currentName();p.nextToken();map.put(name,read(p,depth+1));}yield map;}
            case START_ARRAY->{List<Object> list=new ArrayList<>();while(p.nextToken()!=JsonToken.END_ARRAY){if(list.size()>=200_000)throw new FormatException("JSON array exceeds limit");list.add(read(p,depth+1));}yield List.copyOf(list);}
            case VALUE_STRING->p.getString(); case VALUE_NUMBER_INT->p.getLongValue(); case VALUE_NUMBER_FLOAT->p.getDoubleValue();
            case VALUE_TRUE->true; case VALUE_FALSE->false; case VALUE_NULL->null;
            default->throw new FormatException("Unexpected JSON token "+p.currentToken());
        };
    }
    static final class FormatException extends IllegalArgumentException{FormatException(String m){super(m);}FormatException(String m,Throwable c){super(m,c);}}
}
