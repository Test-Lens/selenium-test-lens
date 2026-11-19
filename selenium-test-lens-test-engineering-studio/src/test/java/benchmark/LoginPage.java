package benchmark;

/** Compile-time Page Object fixture used by Studio workflow boundary tests. */
public final class LoginPage {
    public boolean invalidPasswordShowsExpectedError(String username, String password) {
        return username != null && password != null;
    }
}
