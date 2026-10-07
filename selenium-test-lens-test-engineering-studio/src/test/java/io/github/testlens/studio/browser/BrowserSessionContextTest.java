package io.github.testlens.studio.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class BrowserSessionContextTest {
    @Test void bindingIsThreadScopedAndRemovedOnClose(){
        WebDriver driver=(WebDriver)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{WebDriver.class},(proxy,method,args)->null);
        assertThrows(IllegalStateException.class,BrowserSessionContext::currentDriver);
        try(var ignored=BrowserSessionContext.bind(driver)){assertSame(driver,BrowserSessionContext.currentDriver());assertThrows(IllegalStateException.class,()->BrowserSessionContext.bind(driver));}
        assertThrows(IllegalStateException.class,BrowserSessionContext::currentDriver);
    }
}
