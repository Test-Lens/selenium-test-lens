package io.github.testlens.studio.transport;

import io.github.testlens.studio.TestEngineeringStudioService;
import io.github.testlens.studio.browser.*;
import io.github.testlens.studio.projection.StudioProjections.Capability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestEngineeringStudioServerTest {
    @TempDir Path root;

    @Test void requiresSessionTokenAndExactOriginAndRejectsUnknownActions() throws Exception {
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,List.of(),List.of(),List.of(),"fixture"),null,null);
        try(var server=new TestEngineeringStudioServer(service)){
            server.start();HttpClient client=HttpClient.newHttpClient();URI project=server.uri().resolve("api/project");
            assertEquals(401,client.send(HttpRequest.newBuilder(project).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            var get=HttpRequest.newBuilder(project).header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken()).GET().build();
            assertEquals(200,client.send(get,HttpResponse.BodyHandlers.ofString()).statusCode());
            URI actions=server.uri().resolve("api/actions");
            var wrongOrigin=HttpRequest.newBuilder(actions).header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken()).header("Origin","http://evil.invalid")
                    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"actionId\":\"scan-project\"}")).build();
            assertEquals(403,client.send(wrongOrigin,HttpResponse.BodyHandlers.discarding()).statusCode());
            var forged=HttpRequest.newBuilder(actions).header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken()).header("Origin",server.uri().toString().replaceAll("/$",""))
                    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"actionId\":\"writeFile\",\"path\":\"outside\"}")).build();
            assertEquals(400,client.send(forged,HttpResponse.BodyHandlers.discarding()).statusCode());
            assertTrue(server.uri().getHost().equals("127.0.0.1")||server.uri().getHost().equals("0:0:0:0:0:0:0:1"));
            assertTrue(java.util.Base64.getUrlDecoder().decode(server.sessionToken()).length>=32);
        }
    }

    @Test void reportsBoundedRedactedBrowserSessionCreationDiagnostics() throws Exception {
        BrowserSessionProvider provider=new BrowserSessionProvider(){
            @Override public BrowserAvailability preflight(BrowserRequest request){return BrowserAvailability.AVAILABLE;}
            @Override public BrowserSession open(BrowserRequest request){throw new IllegalStateException("Unable to open local CHROME session",new IllegalStateException("session not created; password=top-secret "+"x".repeat(4_000)));}
        };
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,List.of(),List.of(),List.of(),"fixture"),null,provider,root.resolve(".test-lens"));
        service.attachCapabilities(new Capability("AVAILABLE","test"),new Capability("NOT_AVAILABLE","test"),new Capability("AVAILABLE","test"));
        try(var server=new TestEngineeringStudioServer(service)){
            server.start();HttpClient client=HttpClient.newHttpClient();URI actions=server.uri().resolve("api/actions");
            var request=HttpRequest.newBuilder(actions)
                    .header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken())
                    .header("Origin",server.uri().toString().replaceAll("/$",""))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"MAP_APPLICATION\",\"mode\":\"CURRENT_PAGE\"}"))
                    .build();

            var response=client.send(request,HttpResponse.BodyHandlers.ofString());

            assertEquals(409,response.statusCode());
            assertTrue(response.body().contains("BROWSER_SESSION_FAILED"));
            assertTrue(response.body().contains("IllegalStateException"));
            assertTrue(response.body().contains("session not created"));
            assertTrue(response.body().contains("[REDACTED]"));
            assertFalse(response.body().contains("top-secret"));
            assertTrue(response.body().length()<5_000,"browser failure diagnostics must remain bounded");
            assertTrue(response.body().contains("driverResolution"));
            assertTrue(response.body().contains("headlessMode"));
        }
    }
}
