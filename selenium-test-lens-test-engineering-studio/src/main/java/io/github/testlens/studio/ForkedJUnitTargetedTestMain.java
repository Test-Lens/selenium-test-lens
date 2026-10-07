package io.github.testlens.studio;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.studio.browser.Browser;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.BrowserSession;
import io.github.testlens.studio.browser.BrowserSessionContext;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import io.github.testlens.studio.browser.Ownership;
import io.github.testlens.studio.browser.Purpose;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Internal, versioned subprocess protocol endpoint for targeted execution. */
public final class ForkedJUnitTargetedTestMain {
    private static final int SCHEMA_VERSION=1;
    private static final RedactionPolicy REDACTION=RedactionPolicy.defaults();
    private ForkedJUnitTargetedTestMain() { }

    public static void main(String[] args) {
        if(args.length!=9){System.err.println("Invalid targeted execution request");System.exit(64);}
        Path result=Path.of(args[8]);Properties values=new Properties();values.setProperty("schemaVersion",Integer.toString(SCHEMA_VERSION));
        try {
            BrowserSessionProvider provider=(BrowserSessionProvider)Class.forName(args[0]).getDeclaredConstructor().newInstance();
            BrowserRequest request=new BrowserRequest(Purpose.valueOf(args[1]),Browser.valueOf(args[2]),Ownership.valueOf(args[3]),Boolean.parseBoolean(args[4]),args[5]);
            System.setProperty("testlens.targeted.profile",request.profileId());
            try(BrowserSession session=provider.open(request);BrowserSessionContext.Scope ignored=BrowserSessionContext.bind(session.driver())) {
                if(session.ownership()!=request.ownership())throw new IllegalStateException("Browser provider returned unexpected session ownership");
                if(!args[6].isBlank())session.driver().get(URI.create(args[6]).toString());
                Class<?> testClass=Class.forName(args[7],true,Thread.currentThread().getContextClassLoader());
                var discovery=LauncherDiscoveryRequestBuilder.request().selectors(selectClass(testClass)).build();
                SummaryGeneratingListener listener=new SummaryGeneratingListener();
                var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(discovery);
                var summary=listener.getSummary();List<String> evidence=new ArrayList<>();
                summary.getFailures().stream().limit(20).forEach(failure->evidence.add(failure.getException().getClass().getSimpleName()+": "+bounded(failure.getException().getMessage())));
                values.setProperty("status",summary.getTotalFailureCount()==0&&summary.getTestsSucceededCount()>0?"PASS":"FAIL");
                values.setProperty("tests",Long.toString(summary.getTestsFoundCount()));writeEvidence(values,evidence);
            }
        } catch(Throwable failure) {
            values.setProperty("status","FAIL");values.setProperty("tests","0");
            writeEvidence(values,List.of(failure.getClass().getSimpleName()+": "+bounded(failure.getMessage())));
        }
        try{writeAtomically(result,values);}catch(Exception failure){System.err.println(bounded(failure.getMessage()));System.exit(74);}
    }

    private static String bounded(String value){String clean=REDACTION.redact(String.valueOf(value)).replaceAll("[\\r\\n]+"," ");return clean.length()<=1000?clean:clean.substring(0,1000);}
    private static void writeEvidence(Properties values,List<String> evidence){values.setProperty("evidence.count",Integer.toString(evidence.size()));for(int i=0;i<evidence.size();i++)values.setProperty("evidence."+i,evidence.get(i));}
    private static void writeAtomically(Path target,Properties values)throws Exception{Files.createDirectories(target.getParent());Path temp=target.resolveSibling(target.getFileName()+".tmp");try(var out=Files.newOutputStream(temp)){values.store(out,"Test Lens targeted execution");}try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}
}
