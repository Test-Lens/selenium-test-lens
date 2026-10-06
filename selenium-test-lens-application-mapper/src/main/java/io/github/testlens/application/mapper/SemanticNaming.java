package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationIds;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.CandidateAnalysis;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SemanticNaming {
    private SemanticNaming(){}
    static String pageName(String pattern,String title,ApplicationOverrides overrides){
        String configured=overrides.pageNamesByUrlPattern().get(pattern);if(configured!=null&&!configured.isBlank())return javaIdentifier(configured,"Page");
        String path=pattern.replaceFirst("^[a-z]+://[^/]+","");String[]parts=path.split("/");String base="";
        for(int i=parts.length-1;i>=0;i--)if(!parts[i].isBlank()&&!parts[i].equals("{id}")){base=parts[i];break;}
        if(base.isBlank())base=title==null||title.isBlank()?"Application":title;
        String id=javaIdentifier(base,"Application");return id.endsWith("Page")?id:id+"Page";
    }
    static String elementName(PageScanner.DiscoveredElement e,List<CandidateAnalysis.Candidate> ranked,String fingerprint,
                              ApplicationOverrides overrides,RedactionPolicy redaction){
        return elementName(e,ranked,fingerprint,overrides,redaction,List.of("data-testid","data-test","data-qa"));
    }
    static String elementName(PageScanner.DiscoveredElement e,List<CandidateAnalysis.Candidate> ranked,String fingerprint,
                              ApplicationOverrides overrides,RedactionPolicy redaction,List<String> preferredAttributes){
        String configured=overrides.elementNamesByFingerprint().get(fingerprint);if(configured!=null&&!configured.isBlank())return lowerCamel(configured,"element");
        String test=preferredTestAttribute(e.testAttributes(),preferredAttributes,redaction);
        ApplicationModel.ElementType type=type(e);String leafText=switch(type){case BUTTON,LINK,TAB,MENU_ITEM,OPTION->redaction.redact(e.text());default->null;};
        String base=first(test,redaction.redact(e.accessibleName()),redaction.redact(e.label()),
                attribute(ranked,CandidateAnalysis.Origin.NAME,redaction),attribute(ranked,CandidateAnalysis.Origin.ID,redaction),
                leafText,redaction.redact(e.role()),redaction.redact(e.tag()));
        String suffix=suffix(type);String name=lowerCamel(base,"element");return name.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))?name:name+suffix;
    }
    static String elementName(PageScanner.DiscoveredElement e,List<CandidateAnalysis.Candidate> ranked,String fingerprint,
                              ApplicationOverrides overrides){
        return elementName(e,ranked,fingerprint,overrides,RedactionPolicy.defaults());
    }
    static ApplicationModel.ElementType type(PageScanner.DiscoveredElement e){String role=lower(e.role()),tag=lower(e.tag()),type=lower(e.inputType());
        if(role.equals("button")||tag.equals("button")||type.equals("submit")||type.equals("button"))return ApplicationModel.ElementType.BUTTON;
        if(role.equals("checkbox")||type.equals("checkbox"))return ApplicationModel.ElementType.CHECKBOX;if(role.equals("radio")||type.equals("radio"))return ApplicationModel.ElementType.RADIO;
        if(role.equals("link")||tag.equals("a"))return ApplicationModel.ElementType.LINK;if(role.equals("tab"))return ApplicationModel.ElementType.TAB;
        if(role.equals("menu"))return ApplicationModel.ElementType.MENU;if(role.equals("menuitem"))return ApplicationModel.ElementType.MENU_ITEM;
        if(role.equals("dialog"))return ApplicationModel.ElementType.DIALOG;if(role.equals("combobox"))return ApplicationModel.ElementType.COMBOBOX;
        if(role.equals("listbox"))return ApplicationModel.ElementType.LISTBOX;if(role.equals("option")||tag.equals("option"))return ApplicationModel.ElementType.OPTION;
        if(role.equals("navigation")||tag.equals("nav"))return ApplicationModel.ElementType.NAVIGATION;if(role.equals("search"))return ApplicationModel.ElementType.SEARCH;
        if(tag.equals("textarea"))return ApplicationModel.ElementType.TEXTAREA;if(tag.equals("select"))return ApplicationModel.ElementType.SELECT;
        if(tag.equals("form"))return ApplicationModel.ElementType.FORM;if(tag.equals("table"))return ApplicationModel.ElementType.TABLE;if(tag.equals("input"))return ApplicationModel.ElementType.INPUT;
        return tag!=null&&tag.contains("-")?ApplicationModel.ElementType.CUSTOM:ApplicationModel.ElementType.UNKNOWN;}
    static List<ApplicationModel.Action>actions(ApplicationModel.ElementType type,boolean disabled){if(disabled)return List.of(ApplicationModel.Action.ASSERT);return switch(type){
        case BUTTON,LINK,TAB,MENU_ITEM->List.of(ApplicationModel.Action.CLICK,ApplicationModel.Action.ASSERT);case INPUT,TEXTAREA->List.of(ApplicationModel.Action.TYPE,ApplicationModel.Action.CLEAR,ApplicationModel.Action.FOCUS,ApplicationModel.Action.ASSERT);
        case CHECKBOX->List.of(ApplicationModel.Action.CHECK,ApplicationModel.Action.UNCHECK,ApplicationModel.Action.ASSERT);case RADIO->List.of(ApplicationModel.Action.CHECK,ApplicationModel.Action.ASSERT);
        case SELECT,COMBOBOX,LISTBOX,OPTION->List.of(ApplicationModel.Action.SELECT,ApplicationModel.Action.ASSERT);default->List.of(ApplicationModel.Action.ASSERT);};}
    static List<ApplicationModel.Action>actions(ApplicationModel.ElementType type,String inputType,boolean disabled){
        if(type==ApplicationModel.ElementType.INPUT&&"file".equalsIgnoreCase(inputType)&&!disabled)return List.of(ApplicationModel.Action.UPLOAD,ApplicationModel.Action.ASSERT);
        return actions(type,disabled);
    }
    static String lowerCamel(String value,String fallback){String pascal=javaIdentifier(value,fallback);return Character.toLowerCase(pascal.charAt(0))+pascal.substring(1);}
    static String javaIdentifier(String value,String fallback){String token=ApplicationIds.semanticToken(present(value)?value:fallback);StringBuilder out=new StringBuilder();boolean upper=true;for(char c:token.toCharArray()){if(c=='-'){upper=true;continue;}out.append(upper?Character.toUpperCase(c):c);upper=false;}if(out.isEmpty())out.append(fallback);if(Character.isDigit(out.charAt(0)))out.insert(0,'X');return out.toString();}
    static String preferredTestAttribute(Map<String,String> attributes,List<String> preferred,RedactionPolicy redaction){
        for(String key:preferred){String value=redaction.redact(key,attributes.get(key));if(present(value))return value;}
        return attributes.entrySet().stream().filter(entry->!preferred.contains(entry.getKey())).sorted(Map.Entry.comparingByKey())
                .map(entry->redaction.redact(entry.getKey(),entry.getValue())).filter(SemanticNaming::present).findFirst().orElse(null);
    }
    private static String attribute(List<CandidateAnalysis.Candidate> values,CandidateAnalysis.Origin origin,RedactionPolicy redaction){return values.stream().filter(c->c.origins().contains(origin)&&c.locator()!=null).map(c->redaction.redact(c.locator().value())).findFirst().orElse(null);}
    private static String suffix(ApplicationModel.ElementType type){return switch(type){case BUTTON->"Button";case LINK->"Link";case INPUT->"Input";case TEXTAREA->"Textarea";case SELECT->"Select";case CHECKBOX->"Checkbox";case RADIO->"Radio";case TABLE->"Table";case FORM->"Form";case TAB->"Tab";case DIALOG->"Dialog";default->"";};}
    private static String first(String...values){for(String value:values)if(present(value))return value;return "element";}
    private static boolean present(String value){return value!=null&&!value.isBlank();}private static String lower(String value){return value==null?"":value.toLowerCase(Locale.ROOT);}
}
