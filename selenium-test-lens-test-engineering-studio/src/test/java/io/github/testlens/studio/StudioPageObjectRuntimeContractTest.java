package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StudioPageObjectRuntimeContractTest {
    @Test void resolvesNoArgAndStudioBoundWebDriverConstructorsWithoutGuessingOtherDependencies() {
        AgentContextPack.PageObjectCapability page=capability(List.of(
                constructor("LoginPage()",List.of()),
                constructor("LoginPage(WebDriver)",List.of(parameter("driver","org.openqa.selenium.WebDriver"))),
                constructor("LoginPage(project.WebDriver)",List.of(parameter("driver","WebDriver"))),
                constructor("LoginPage(Config)",List.of(parameter("configuration","Config")))));

        String contract=StudioPageObjectRuntimeContract.render(context(page));

        assertTrue(contract.contains("DIRECT_NO_ARGS"));
        assertTrue(contract.contains("new benchmark.LoginPage()"));
        assertTrue(contract.contains("STUDIO_BOUND_WEBDRIVER"));
        assertTrue(contract.contains("BrowserSessionContext.currentDriver()"));
        assertTrue(contract.contains("UNRESOLVED_DEPENDENCIES"));
        assertEquals(1, occurrences(contract, "STUDIO_BOUND_WEBDRIVER"),
                "only the exact Selenium WebDriver type may use the bound Studio driver");
        assertFalse(contract.contains("new benchmark.LoginPage(configuration)"));
    }

    @Test void doesNotPromoteInaccessibleOrUnverifiedConstructors() {
        AgentContextPack.ConstructorCapability constructor=new AgentContextPack.ConstructorCapability(
                "LoginPage()",List.of(),"PROTECTED",null,false);

        String contract=StudioPageObjectRuntimeContract.render(context(capability(List.of(constructor))));

        assertTrue(contract.contains("UNSUPPORTED_OR_INACCESSIBLE"));
        assertFalse(contract.contains("constructionExpression"));
    }

    private static AgentContextPack.ConstructorCapability constructor(String signature,
            List<AgentContextPack.CapabilityParameter> parameters) {
        return new AgentContextPack.ConstructorCapability(signature,parameters,"PUBLIC","DIRECT_CONSTRUCTION",false);
    }
    private static AgentContextPack.CapabilityParameter parameter(String name,String type) {
        return new AgentContextPack.CapabilityParameter(name,type);
    }
    private static AgentContextPack.PageObjectCapability capability(
            List<AgentContextPack.ConstructorCapability> constructors) {
        return new AgentContextPack.PageObjectCapability("login-page","login","benchmark.LoginPage",
                "src/test/java/benchmark/LoginPage.java","PUBLIC",constructors,List.of());
    }
    private static AgentContextPack context(AgentContextPack.PageObjectCapability capability) {
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,
                List.of(),List.of(),ContractHeader.Confidence.OBSERVED);
        AgentTask task=new AgentTask(header,AgentTask.TaskType.CREATE_TEST,"test",List.of("login"),List.of(),
                List.of(),List.of());
        return new AgentContextPack(header,task,List.of(),Map.of(),List.of(),AgentContextPack.SourceContext.empty(),
                new AgentContextPack.PageObjectCapabilityManifest("fingerprint",
                        AgentContextPack.ManifestCompleteness.COMPLETE,List.of(capability),List.of()),
                List.of(),List.of(),AgentContextPack.Completeness.PARTIAL);
    }
    private static int occurrences(String value,String token) {
        return value.split(java.util.regex.Pattern.quote(token),-1).length-1;
    }
}
