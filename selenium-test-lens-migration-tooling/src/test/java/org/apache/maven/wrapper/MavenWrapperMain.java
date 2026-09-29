package org.apache.maven.wrapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Harmless local fixture for exercising the fixed Maven wrapper-main process boundary. */
public final class MavenWrapperMain {
    private MavenWrapperMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !"--literal".equals(args[0]) || !"$NOT_EXPANDED".equals(args[1])) {
            System.err.println("unexpected literal arguments: " + List.of(args));
            System.exit(17);
        }
        Files.writeString(Path.of(args[2]), "wrapper-main-ok\n", StandardCharsets.UTF_8);
    }
}
