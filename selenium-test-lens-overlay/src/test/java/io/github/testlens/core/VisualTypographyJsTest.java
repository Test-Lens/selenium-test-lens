package io.github.testlens.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualTypographyJsTest {

    @Test
    void bundlesVariableWoff2AndItsOflLicense() throws IOException {
        byte[] font;
        try (var input = getClass().getClassLoader().getResourceAsStream(VisualTypographyJs.FONT_RESOURCE)) {
            assertNotNull(input);
            font = input.readAllBytes();
        }
        assertTrue(font.length > 0);
        assertArrayEquals(new byte[]{'w', 'O', 'F', '2'}, java.util.Arrays.copyOf(font, 4));

        try (var license = getClass().getClassLoader().getResourceAsStream(VisualTypographyJs.LICENSE_RESOURCE)) {
            assertNotNull(license);
            String text = new String(license.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(text.contains("SIL OPEN FONT LICENSE Version 1.1"));
            assertTrue(text.contains("The Sora Project Authors"));
        }
    }

    @Test
    void definesOneSharedLocalUiStackWithoutChangingSystemOrMonospace() {
        assertEquals("\"Test Lens Sora\", system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif",
                VisualTypographyJs.UI_FONT_STACK);
        assertTrue(VisualTypographyJs.INIT.contains("weight: '100 800'"));
        assertTrue(VisualTypographyJs.INIT.contains("new window.FontFace"));
        assertTrue(VisualTypographyJs.INIT.contains("if (state.loadPromise) return state.loadPromise"));
        assertTrue(VisualTypographyJs.INIT.contains("system-ui, -apple-system"));
        assertTrue(VisualTypographyJs.INIT.contains("ui-monospace, SFMono-Regular"));
        assertFalse(VisualTypographyJs.INIT.contains("fonts.googleapis.com"));
        assertFalse(VisualTypographyJs.INIT.contains("fonts.gstatic.com"));
    }

    @Test
    void repeatedInjectionCreatesOneFontFaceAndFailureRemainsBounded(@TempDir Path directory) throws Exception {
        Path script = directory.resolve("visual-typography-idempotency.js");
        String runtime = jsonString(VisualTypographyJs.INIT);
        String test = """
                let faces=0, adds=0;
                const styles=[];
                const root={querySelector:s=>styles.find(x=>x.marker)||null,appendChild:x=>{styles.push(x);}};
                const document={currentScript:null,fonts:{add(){adds++;},delete(){return true;}},
                  createElement(){return {setAttribute(n){if(n==='data-test-lens-visual-typography')this.marker=true;},textContent:''};}};
                const window={document,atob:globalThis.atob,Uint8Array,Promise,setTimeout,
                  FontFace:function(){faces++;this.load=()=>Promise.reject(new Error('blocked'));}};
                window.window=window;
                const runtime=%s;
                eval(runtime);eval(runtime);
                window.__uiTestLens.modules.visualTypography.ensureRoot(root);
                window.__uiTestLens.modules.visualTypography.ensureRoot(root);
                setImmediate(()=>{
                  if(faces!==1||adds!==1||styles.length!==1)process.exit(2);
                  if(window.__uiTestLens.modules.visualTypography.status()!=='failed')process.exit(3);
                });
                """.formatted(runtime);
        Files.writeString(script, test, StandardCharsets.UTF_8);

        Process process = new ProcessBuilder("node", script.toString()).redirectErrorStream(true).start();
        assertTrue(process.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') escaped.append('\\').append(c);
            else if (c == '\n') escaped.append("\\n");
            else if (c == '\r') escaped.append("\\r");
            else if (c == '\t') escaped.append("\\t");
            else escaped.append(c);
        }
        return escaped.append('"').toString();
    }
}
