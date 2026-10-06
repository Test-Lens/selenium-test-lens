package io.github.testlens.application.mapper;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.*;

final class PageScanner {
    private static final String DISCOVER="""
        const max=arguments[0],maxRegions=arguments[1],maxShadowDepth=arguments[2],testAttrs=arguments[3];
        const selector='button,input:not([type="hidden"]),textarea,select,a[href],form,table,[role="button"],[role="checkbox"],[role="radio"],[role="link"],[role="tab"],[role="menu"],[role="menuitem"],[role="dialog"],[role="combobox"],[role="listbox"],[role="option"],[role="navigation"],[role="search"],[data-component]';
        const regionSelector='header,nav,main,aside,footer,form,[role="region"],[role="navigation"],[role="main"],[role="dialog"],[data-component]';
        const out=[],seen=new Set(),regions=new Map(),roots=[{root:document,depth:0,hostPath:''}];let discovered=0,closedPossible=0,shadowRoots=0,depthReached=0,truncated=false;
        function cut(v,n=256){if(v==null)return null;return Array.from(String(v)).slice(0,n).join('');}
        function visible(e){const s=getComputedStyle(e);return s.display!=='none'&&s.visibility!=='hidden'&&s.opacity!=='0'&&e.getClientRects().length>0&&!e.hidden;}
        function region(e){let r=e.closest(regionSelector);if(!r)return null;let role=r.getAttribute('role')||r.tagName.toLowerCase();let name=r.getAttribute('aria-label')||r.getAttribute('data-component')||r.id||role;let key=role+'|'+name;if(!regions.has(key)&&regions.size<maxRegions)regions.set(key,{key:key,role:cut(role,64),name:cut(name),tag:cut(r.tagName.toLowerCase(),64)});return regions.has(key)?key:null;}
        function label(e){if(e.labels&&e.labels.length)return cut(Array.from(e.labels).map(x=>x.innerText||x.textContent||'').join(' '));let id=e.id;if(id){let l=document.querySelector('label[for="'+CSS.escape(id)+'"]');if(l)return cut(l.innerText||l.textContent);}return null;}
        function segment(e){let tag=e.tagName.toLowerCase(),role=e.getAttribute('role')||'',component=e.getAttribute('data-component')||'',same=0,index=0;for(let s=e.parentElement&&e.parentElement.firstElementChild;s;s=s.nextElementSibling){if(s.tagName===e.tagName){same++;if(s===e)index=same;}}return tag+'['+index+']'+'|'+role+'|'+component;}
        function structural(e,hostPath){let parts=[],n=e;while(n&&n.nodeType===1&&parts.length<5){parts.unshift(segment(n));n=n.parentElement;}return cut((hostPath?hostPath+'/':'')+parts.join('/'),1024);}
        function semanticText(e){let tag=e.tagName.toLowerCase(),role=(e.getAttribute('role')||'').toLowerCase();if(['input','textarea','select','form','table'].includes(tag)||['dialog','navigation','search','menu','listbox'].includes(role))return null;return cut((e.innerText||e.textContent||'').trim());}
        outer:while(roots.length){let item=roots.pop(),root=item.root,depth=item.depth;depthReached=Math.max(depthReached,depth);let walker=document.createTreeWalker(root,NodeFilter.SHOW_ELEMENT);let e;
          while((e=walker.nextNode())){if(discovered>=max){truncated=true;break outer;}discovered++;
            if(e.tagName.includes('-')&&!e.shadowRoot)closedPossible++;
            if(e.shadowRoot){shadowRoots++;if(depth<maxShadowDepth)roots.push({root:e.shadowRoot,depth:depth+1,hostPath:structural(e,item.hostPath)});else depthReached=Math.max(depthReached,depth+1);}
            if(!e.matches(selector)||seen.has(e)||!visible(e))continue;seen.add(e);let attrs={};for(const n of testAttrs){let v=e.getAttribute(n);if(v!==null)attrs[n]=cut(v,512);}out.push({element:e,context:root===document?null:root,shadow:root!==document,tag:cut(e.tagName.toLowerCase(),64),type:cut(e.getAttribute('type'),64),role:cut(e.getAttribute('role')||'',64),label:label(e),accessibleName:cut(e.getAttribute('aria-label')||''),text:semanticText(e),targetUrl:e.tagName.toLowerCase()==='a'?cut(e.href,2048):null,disabled:!!e.disabled||e.getAttribute('aria-disabled')==='true',regionKey:region(e),structuralHint:structural(e,item.hostPath),testAttributes:attrs});}
        }
        return {elements:out,regions:Array.from(regions.values()),discovered:discovered,truncated:truncated,closedShadowPossible:closedPossible,shadowRoots:shadowRoots,shadowDepthReached:depthReached};
        """;
    Snapshot scan(WebDriver driver,ApplicationMapperOptions options){
        if(!(driver instanceof JavascriptExecutor js))throw new MappingException(MappingException.Code.BROWSER_SCRIPT_FAILED,"JavascriptExecutor unavailable");
        long started=System.nanoTime();Object raw;
        try{raw=js.executeScript(DISCOVER,options.maxDiscoveredNodes(),options.maxRegions(),options.maxShadowDepth(),options.preferredTestAttributes());}
        catch(WebDriverException failure){throw new MappingException(MappingException.Code.BROWSER_SCRIPT_FAILED,"Page discovery failed",failure);}
        if(!(raw instanceof Map<?,?>map))throw new MappingException(MappingException.Code.BROWSER_SCRIPT_FAILED,"Unexpected discovery response");
        List<DiscoveredElement>elements=new ArrayList<>();if(map.get("elements")instanceof List<?>list)for(Object value:list){if(!(value instanceof Map<?,?>e)||!(e.get("element")instanceof WebElement target))continue;SearchContext context=e.get("context")instanceof SearchContext supplied?supplied:driver;elements.add(new DiscoveredElement(target,context,Boolean.TRUE.equals(e.get("shadow")),s(e.get("tag")),s(e.get("type")),s(e.get("role")),s(e.get("label")),s(e.get("accessibleName")),s(e.get("text")),s(e.get("targetUrl")),Boolean.TRUE.equals(e.get("disabled")),s(e.get("regionKey")),s(e.get("structuralHint")),stringMap(e.get("testAttributes"))));}
        List<DiscoveredRegion>regions=new ArrayList<>();if(map.get("regions")instanceof List<?>list)for(Object value:list)if(value instanceof Map<?,?>r)regions.add(new DiscoveredRegion(s(r.get("key")),s(r.get("role")),s(r.get("name")),s(r.get("tag"))));
        return new Snapshot(List.copyOf(elements),List.copyOf(regions),n(map.get("discovered")),Boolean.TRUE.equals(map.get("truncated")),n(map.get("closedShadowPossible")),n(map.get("shadowRoots")),n(map.get("shadowDepthReached")),System.nanoTime()-started);
    }
    private static String s(Object value){return value==null?null:String.valueOf(value);}
    private static int n(Object value){return value instanceof Number number?number.intValue():0;}
    private static Map<String,String>stringMap(Object raw){if(!(raw instanceof Map<?,?>map))return Map.of();Map<String,String>out=new TreeMap<>();map.forEach((k,v)->{if(k!=null&&v!=null)out.put(String.valueOf(k),String.valueOf(v));});return Map.copyOf(out);}
    record Snapshot(List<DiscoveredElement>elements,List<DiscoveredRegion>regions,int discoveredNodes,boolean truncated,int closedShadowPossible,int shadowRoots,int shadowDepthReached,long discoveryNanos){}
    record DiscoveredElement(WebElement target,SearchContext context,boolean shadow,String tag,String inputType,String role,String label,String accessibleName,String text,String targetUrl,boolean disabled,String regionKey,String structuralHint,Map<String,String>testAttributes){
        DiscoveredElement(WebElement target,SearchContext context,boolean shadow,String tag,String inputType,String role,String label,String accessibleName,String text,String targetUrl,boolean disabled,String regionKey,Map<String,String>testAttributes){this(target,context,shadow,tag,inputType,role,label,accessibleName,text,targetUrl,disabled,regionKey,null,testAttributes);}
    }
    record DiscoveredRegion(String key,String role,String name,String tag){}
}
