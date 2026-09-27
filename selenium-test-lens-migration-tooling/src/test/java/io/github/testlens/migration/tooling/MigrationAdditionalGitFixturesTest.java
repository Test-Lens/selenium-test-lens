package io.github.testlens.migration.tooling;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationAdditionalGitFixturesTest {
    @TempDir Path temp;

    @Test void listsMultipleWorktreesWithoutChangingThem() throws Exception {
        Path repository = repository("multiple-worktrees");
        write(repository, "tracked.txt", "base\n");
        commitAll(repository, "base");
        Path additional = temp.resolve("additional worktree");
        git(repository, "worktree", "add", "-b", "fixture/additional", additional.toString());

        var preflight = new MigrationGitService().inspect(repository).preflight();
        assertEquals(2, preflight.worktrees().size());
        assertEquals(1, preflight.worktrees().stream().filter(MigrationGitPreflight.Worktree::current).count());
        assertNotEquals(preflight.worktrees().get(0).bindingRef(), preflight.worktrees().get(1).bindingRef());
        assertTrue(Files.exists(additional.resolve("tracked.txt")));
    }

    @Test void detectsSparseCheckoutWhenSupported() throws Exception {
        Path repository = repository("sparse");
        write(repository, "kept/a.txt", "a\n");
        write(repository, "other/b.txt", "b\n");
        commitAll(repository, "base");
        int exit = gitExit(repository, "sparse-checkout", "init", "--cone");
        Assumptions.assumeTrue(exit == 0, "local Git does not support sparse-checkout");

        assertEquals(MigrationGitPreflight.SparseCheckout.ENABLED,
                new MigrationGitService().inspect(repository).preflight().sparseCheckout());
    }

    @Test void preservesPortableWeirdNamesThroughRealPorcelain() throws Exception {
        Path repository = repository("weird-names");
        write(repository, "space and unicode-żółć.txt", "a");
        write(repository, "-leading-dash.txt", "b");

        var paths = new MigrationGitService().inspect(repository).preflight().untracked().stream()
                .map(MigrationGitPreflight.StatusEntry::logicalPath).toList();
        assertTrue(paths.contains("space and unicode-żółć.txt"));
        assertTrue(paths.contains("-leading-dash.txt"));
    }

    @Test void hashesSymlinkTargetWithoutFollowingWhenPlatformAllowsIt() throws Exception {
        Path repository = repository("symlink");
        Path outside = temp.resolve("outside-secret.txt");
        Files.writeString(outside, "must not be read", StandardCharsets.UTF_8);
        Path link = repository.resolve("external-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (Exception unsupported) {
            Assumptions.abort("symlink creation unavailable: " + unsupported.getClass().getSimpleName());
        }

        var file = new MigrationGitService().inspect(repository).sourceState().files().stream()
                .filter(state -> state.logicalPath().equals("external-link")).findFirst().orElseThrow();
        assertEquals(MigrationSourceStateFingerprint.WorktreeState.SYMLINK, file.worktree().state());
        assertTrue(file.worktree().linkTargetDigest().matches("[0-9a-f]{64}"));
        assertTrue(file.worktree().size() > 0);
    }

    @Test void detectsRealRebaseConflictMarker() throws Exception {
        Path repository = repository("rebase-operation");write(repository,"a.txt","base\n");commitAll(repository,"base");String main=gitText(repository,"branch","--show-current");git(repository,"checkout","-b","side");write(repository,"a.txt","side\n");commitAll(repository,"side");git(repository,"checkout",main);write(repository,"a.txt","main\n");commitAll(repository,"main");git(repository,"checkout","side");assertNotEquals(0,gitExit(repository,"rebase",main));var operations=new MigrationGitService().inspect(repository).preflight().ongoingOperations();assertTrue(operations.contains(MigrationGitPreflight.GitOperation.REBASE_MERGE)||operations.contains(MigrationGitPreflight.GitOperation.REBASE_APPLY));
    }

    @Test void detectsRealCherryPickConflictMarker() throws Exception {
        Path repository = repository("cherry-pick-operation");write(repository,"a.txt","base\n");commitAll(repository,"base");String main=gitText(repository,"branch","--show-current");git(repository,"checkout","-b","side");write(repository,"a.txt","side\n");commitAll(repository,"side");String picked=gitText(repository,"rev-parse","HEAD");git(repository,"checkout",main);write(repository,"a.txt","main\n");commitAll(repository,"main");assertNotEquals(0,gitExit(repository,"cherry-pick",picked));assertTrue(new MigrationGitService().inspect(repository).preflight().ongoingOperations().contains(MigrationGitPreflight.GitOperation.CHERRY_PICK));
    }

    @Test void detectsRealRevertConflictMarker() throws Exception {
        Path repository = repository("revert-operation");write(repository,"a.txt","base\n");commitAll(repository,"base");write(repository,"a.txt","target\n");commitAll(repository,"target");String target=gitText(repository,"rev-parse","HEAD");write(repository,"a.txt","later\n");commitAll(repository,"later");assertNotEquals(0,gitExit(repository,"revert","--no-edit",target));assertTrue(new MigrationGitService().inspect(repository).preflight().ongoingOperations().contains(MigrationGitPreflight.GitOperation.REVERT));
    }

    private Path repository(String name) throws Exception {
        Path repository = temp.resolve(name);
        Files.createDirectory(repository);
        git(repository, "init");
        git(repository, "config", "user.email", "test@example.invalid");
        git(repository, "config", "user.name", "Test");
        return repository;
    }

    private static void write(Path root, String logicalPath, String value) throws Exception {
        Path file = root.resolve(logicalPath);
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, value, StandardCharsets.UTF_8);
    }

    private static void commitAll(Path repository, String message) throws Exception {
        git(repository, "add", "--all");
        git(repository, "commit", "-m", message);
    }

    private static void git(Path repository, String... arguments) throws Exception {
        int exit = gitExit(repository, arguments);
        if (exit != 0) throw new AssertionError("git failed: " + String.join(" ", arguments));
    }

    private static int gitExit(Path repository, String... arguments) throws Exception {
        var command = new ArrayList<String>();
        command.add("git");
        command.addAll(List.of(arguments));
        Process process = new ProcessBuilder(command).directory(repository.toFile()).redirectErrorStream(true).start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        return process.waitFor();
    }

    private static String gitText(Path repository,String...arguments)throws Exception{var command=new ArrayList<String>();command.add("git");command.addAll(List.of(arguments));Process process=new ProcessBuilder(command).directory(repository.toFile()).redirectErrorStream(true).start();String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8).strip();if(process.waitFor()!=0)throw new AssertionError("git failed: "+String.join(" ",arguments));return output;}
}
