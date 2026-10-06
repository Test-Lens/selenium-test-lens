package io.github.testlens.application.mapper;

import io.github.testlens.core.redaction.RedactionPolicy;

import java.net.URI;
import java.net.URISyntaxException;

final class UrlPatternNormalizer {
    private UrlPatternNormalizer(){}
    static String normalize(String raw,RedactionPolicy redaction){
        String safe=redaction.redactUrl(raw);
        try{
            URI uri=new URI(safe);String path=uri.getPath()==null||uri.getPath().isBlank()?"/":uri.getPath();
            String[]parts=path.split("/",-1);StringBuilder normalized=new StringBuilder();
            for(String part:parts){if(part.isEmpty())continue;normalized.append('/').append(dynamic(part)?"{id}":part);}
            if(normalized.length()==0)normalized.append('/');
            return (uri.getScheme()==null?"":uri.getScheme()+"://"+(uri.getHost()==null?"":uri.getHost())+(uri.getPort()<0?"":":"+uri.getPort()))+normalized;
        }catch(URISyntaxException failure){return "url-pattern:sha256:"+io.github.testlens.application.model.ApplicationIds.id("url-v1",safe).substring("url-v1:sha256:".length());}
    }
    private static boolean dynamic(String value){return value.matches("[0-9]{2,}")||value.matches("(?i)[0-9a-f]{8}-[0-9a-f-]{27,}")||value.matches("(?i)[0-9a-f]{20,}");}
}
