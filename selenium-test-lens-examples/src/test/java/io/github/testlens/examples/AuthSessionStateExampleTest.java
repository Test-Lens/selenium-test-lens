package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.auth.AuthRestoreOptions;
import io.github.testlens.selenium.auth.AuthState;
import io.github.testlens.selenium.auth.AuthStateOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

class AuthSessionStateExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver and authenticated app.")
    @Test
    void authSessionStateUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        AuthState state = overlay.auth().captureState(AuthStateOptions.builder()
                .label("standard-customer")
                .role("customer")
                .origin("https://app.example.com")
                .build());

        state.save(Path.of("target/ui-test-lens/auth/customer.json"));

        AuthState restored = AuthState.load(Path.of("target/ui-test-lens/auth/customer.json"));

        overlay.auth().restoreState(restored, AuthRestoreOptions.builder()
                .navigateToOrigin(true)
                .build());
    }
}

