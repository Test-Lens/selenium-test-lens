package io.github.testlens.studio;

import io.github.testlens.studio.browser.BrowserAvailability;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.BrowserSession;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Fork-visible deterministic provider for subprocess lifecycle tests. */
public final class ForkedTestBrowserSessionProvider implements BrowserSessionProvider {
    public ForkedTestBrowserSessionProvider() { }

    @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
    @Override public BrowserSession open(BrowserRequest request){
        if(request.profileId().startsWith("slow-open-"))while(true)Thread.onSpinWait();
        event(request.profileId(),"OPEN");
        WebDriver driver=(WebDriver)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{WebDriver.class},
                (proxy,method,args)->{
                    if(method.getName().equals("quit")){event(request.profileId(),"CLOSE");return null;}
                    if(method.getName().equals("toString"))return "forked-test-driver";
                    Class<?> type=method.getReturnType();if(!type.isPrimitive())return null;
                    if(type==boolean.class)return false;if(type==char.class)return '\0';return 0;
                });
        return new BrowserSession(driver,request.ownership());
    }

    static Path events(String profile){return Path.of(System.getProperty("java.io.tmpdir"),"test-lens-"+profile+".events");}
    private static void event(String profile,String event){try{Files.writeString(events(profile),event+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception failure){throw new IllegalStateException(failure);}}
}
