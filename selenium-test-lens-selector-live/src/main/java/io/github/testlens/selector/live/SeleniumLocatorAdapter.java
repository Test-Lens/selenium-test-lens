package io.github.testlens.selector.live;

import io.github.testlens.selector.engine.CandidateAnalysis.Locator;
import org.openqa.selenium.By;

final class SeleniumLocatorAdapter {
    private SeleniumLocatorAdapter() { }
    static By toBy(Locator locator){return switch(locator.strategy()){
        case "id"->By.id(locator.value());case "css selector"->By.cssSelector(locator.value());case "xpath"->By.xpath(locator.value());
        case "name"->By.name(locator.value());case "class name"->By.className(locator.value());case "tag name"->By.tagName(locator.value());
        case "link text"->By.linkText(locator.value());case "partial link text"->By.partialLinkText(locator.value());
        default->throw new IllegalArgumentException("Unsupported Selenium strategy: "+locator.strategy());};}
    static Locator structural(By by){
        if(!(by instanceof By.Remotable remotable))return null;
        By.Remotable.Parameters parameters=remotable.getRemoteParameters();Object value=parameters.value();
        return value instanceof String scalar?new Locator(parameters.using(),scalar):null;
    }
}
