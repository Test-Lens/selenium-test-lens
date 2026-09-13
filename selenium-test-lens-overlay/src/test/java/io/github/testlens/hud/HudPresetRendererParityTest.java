package io.github.testlens.hud;

import io.github.testlens.core.HudPanelJs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPresetRendererParityTest {
    @TempDir Path temp;

    @Test
    void browserPresetFixtureMatchesEveryJavaPreset() throws Exception {
        Map<String, Object> expected = new LinkedHashMap<>();
        for (HudPreset preset : HudPreset.values()) {
            expected.put(preset.name(), presetFields(HudOptions.builder().preset(preset).build()));
        }
        String script = "var window={__uiTestLens:{modules:{},state:{overlay:{},hud:{}}}},document={};"
                + HudPanelJs.INIT
                + ";function canonical(value){if(!value||typeof value!=='object')return value;var result={};Object.keys(value).sort().forEach(function(key){result[key]=canonical(value[key]);});return result;}"
                + "var actual={};Object.keys(" + json(expected) + ").forEach(function(name){actual[name]=window.__uiTestLens.modules.hud.preset(name);});"
                + "if(JSON.stringify(canonical(actual))!==JSON.stringify(canonical(" + json(expected) + "))){console.error(JSON.stringify(actual));process.exit(2);}";
        Process process;
        try {
            Path validation = temp.resolve("hud-preset-parity.js");
            Files.writeString(validation, script, StandardCharsets.UTF_8);
            process = new ProcessBuilder("node", validation.toString()).start();
        } catch (IOException unavailable) {
            throw new AssertionError("Node.js is required for HUD preset parity validation", unavailable);
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "HUD preset parity validation timed out");
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), errors);
    }

    private static Map<String, Object> presetFields(HudOptions value) {
        Map<String, Object> all = value.toRuntimeMap();
        Map<String, Object> result = new LinkedHashMap<>(all);
        result.remove("preset");
        result.remove("customLogo");
        return result;
    }

    private static String json(Object value) {
        if (value instanceof Map<?, ?> map) {
            StringBuilder result = new StringBuilder("{");
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (result.length() > 1) result.append(',');
                result.append(json(entry.getKey().toString())).append(':').append(json(entry.getValue()));
            }
            return result.append('}').toString();
        }
        if (value instanceof String text) return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        return String.valueOf(value);
    }
}
