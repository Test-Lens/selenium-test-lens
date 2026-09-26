package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.PolicyWorkspaceSnapshot;
import io.github.testlens.selector.engine.SelectorPolicy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** Trusted tooling loader for the complete tracked + local policy workspace. */
public final class SelectorPolicyWorkspace {
    public static final Path TRACKED = Path.of(".test-lens", "selector-policies.json");
    public static final Path LOCAL = Path.of("target", "test-lens", "selector-policies.local.json");

    public PolicyWorkspaceSnapshot load(Path projectRoot, String projectFingerprint) throws IOException {
        Path root = canonicalRoot(projectRoot);
        return PolicyWorkspaceSnapshot.create(read(root, TRACKED, PolicyWorkspaceSnapshot.Origin.TRACKED),
                read(root, LOCAL, PolicyWorkspaceSnapshot.Origin.LOCAL), projectFingerprint);
    }

    static Path canonicalRoot(Path projectRoot) throws IOException {
        if (projectRoot == null) throw new IllegalArgumentException("Trusted projectRoot is required");
        return projectRoot.toAbsolutePath().normalize().toRealPath();
    }

    static Path destination(Path root, PolicyWorkspaceSnapshot.Origin origin) {
        return root.resolve(origin == PolicyWorkspaceSnapshot.Origin.TRACKED ? TRACKED : LOCAL).normalize();
    }

    private static PolicyWorkspaceSnapshot.OriginDocument read(Path root, Path relative,
                                                                 PolicyWorkspaceSnapshot.Origin origin) throws IOException {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Policy path escapes projectRoot");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return PolicyWorkspaceSnapshot.OriginDocument.absent(origin);
        rejectLinkEscape(root, path);
        byte[] bytes = Files.readAllBytes(path);
        SelectorPolicy.Document document = SelectorPolicyJson.parse(bytes);
        List<SelectorPolicy.Rule> rules = document.canonical().rules();
        return new PolicyWorkspaceSnapshot.OriginDocument(origin, PolicyWorkspaceSnapshot.FileState.EXPECTED_PRESENT,
                PolicyWorkspaceSnapshot.rawFileDigest(bytes), PolicyWorkspaceSnapshot.semanticDigest(rules), rules);
    }

    static void rejectLinkEscape(Path root, Path target) throws IOException {
        if (Files.isSymbolicLink(target)) throw new IllegalArgumentException("Policy destination may not be a symbolic link");
        Path parent = target.getParent();
        if (parent != null && Files.exists(parent, LinkOption.NOFOLLOW_LINKS) && !parent.toRealPath().startsWith(root))
            throw new IllegalArgumentException("Policy destination escapes projectRoot through a link");
    }
}
