package io.github.testlens.selector.lab;

import io.github.testlens.selector.engine.CandidateAnalysis;

final class JavaLocatorSnippet {
    private JavaLocatorSnippet(){}
    static String render(CandidateAnalysis.Locator locator){
        if(locator==null)return null;
        String method=switch(locator.strategy()){case"id"->"id";case"css selector"->"cssSelector";case"name"->"name";case"class name"->"className";case"tag name"->"tagName";case"link text"->"linkText";case"xpath"->"xpath";default->null;};
        return method==null?null:"By."+method+"(\""+escape(locator.value())+"\")";
    }
    static String escape(String value){StringBuilder out=new StringBuilder();value.codePoints().forEach(cp->{switch(cp){case'\\'->out.append("\\\\");case'\"'->out.append("\\\"");case'\n'->out.append("\\n");case'\r'->out.append("\\r");case'\t'->out.append("\\t");case'\b'->out.append("\\b");case'\f'->out.append("\\f");default->{if(cp<0x20||cp==0x7f)out.append(String.format("\\u%04x",cp));else out.appendCodePoint(cp);}}});return out.toString();}
}
