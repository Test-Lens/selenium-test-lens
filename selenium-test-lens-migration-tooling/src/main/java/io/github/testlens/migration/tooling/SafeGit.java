package io.github.testlens.migration.tooling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Closed read-only Git command dispatcher. */
final class SafeGit {
    enum Operation{VERSION,ROOT,COMMON_DIR,OBJECT_FORMAT,HEAD_COMMIT,HEAD_TREE,READ_SYMBOLIC_HEAD,STATUS,WORKTREES,INDEX,READ_BLOB,GIT_PATH,SPARSE,CHECK_BRANCH,CHECK_IGNORE}
    private static final Set<String>MARKERS=Set.of("MERGE_HEAD","rebase-merge","rebase-apply","CHERRY_PICK_HEAD","REVERT_HEAD","BISECT_LOG");
    private final String executable;private final GitProcessRunner runner;private final Duration timeout;private final int outputLimit;
    SafeGit(String executable){this(executable,new GitProcessRunner(),Duration.ofSeconds(30),GitProcessRunner.DEFAULT_OUTPUT_LIMIT);}
    SafeGit(String executable,GitProcessRunner runner,Duration timeout,int outputLimit){this.executable=trustedExecutable(executable);this.runner=runner;this.timeout=timeout;this.outputLimit=outputLimit;}
    GitProcessRunner.Result execute(Operation operation,Path cwd,String value)throws IOException{
        List<String>args=switch(operation){
            case VERSION->List.of("--version");case ROOT->List.of("rev-parse","--show-toplevel");case COMMON_DIR->List.of("rev-parse","--git-common-dir");
            case OBJECT_FORMAT->List.of("rev-parse","--show-object-format");case HEAD_COMMIT->List.of("rev-parse","--verify","HEAD^{commit}");case HEAD_TREE->List.of("rev-parse","HEAD^{tree}");
            case READ_SYMBOLIC_HEAD->List.of("symbolic-ref","--quiet","--short","HEAD");case STATUS->List.of("status","--porcelain=v2","-z","--branch","--untracked-files=all");
            case WORKTREES->List.of("worktree","list","--porcelain","-z");
            case INDEX->List.of("ls-files","--stage","-z");case READ_BLOB->List.of("cat-file","blob",oid(value));case GIT_PATH->List.of("rev-parse","--git-path",marker(value));
            case SPARSE->List.of("config","--get","core.sparseCheckout");case CHECK_BRANCH->List.of("check-ref-format","--branch",safeBranch(value));
            case CHECK_IGNORE->List.of("check-ignore","-q","--",safePath(value));};
        List<String>command=new ArrayList<>();command.add(executable);command.add("-c");command.add("core.fsmonitor=false");command.add("-c");command.add("color.ui=false");command.add("--no-pager");command.addAll(args);
        return runner.run(command,cwd,environment(),timeout,outputLimit);
    }
    static Map<String,String>environment(){return Map.of("GIT_TERMINAL_PROMPT","0","GIT_OPTIONAL_LOCKS","0","GIT_PAGER","cat","PAGER","cat","LC_ALL","C");}
    static String text(GitProcessRunner.Result result){return new String(result.stdout(),StandardCharsets.UTF_8).strip();}
    static void requireComplete(GitProcessRunner.Result result,String operation)throws GitFailure{
        if(result.timedOut())throw new GitFailure("GIT_TIMEOUT",operation);if(result.interrupted())throw new GitFailure("GIT_INTERRUPTED",operation);if(result.outputTruncated())throw new GitFailure("GIT_OUTPUT_LIMIT",operation);if(result.exitCode()!=0)throw new GitFailure("GIT_COMMAND_FAILED",operation);
    }
    private static String trustedExecutable(String value){if(value==null||value.isBlank()||"git".equals(value))return"git";Path path=Path.of(value);if(!path.isAbsolute()||!Files.isRegularFile(path))throw new IllegalArgumentException("Explicit Git executable must be an absolute regular file");return path.normalize().toString();}
    private static String oid(String v){if(v==null||!v.matches("[0-9a-fA-F]{40}|[0-9a-fA-F]{64}"))throw new IllegalArgumentException("Invalid Git object id");return v.toLowerCase();}
    private static String marker(String v){if(!MARKERS.contains(v))throw new IllegalArgumentException("Unsupported Git marker");return v;}
    private static String safeBranch(String v){if(v==null||v.isBlank()||v.length()>512||v.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid branch candidate");return v;}
    private static String safePath(String v){if(v==null||v.isBlank()||v.indexOf('\0')>=0)throw new IllegalArgumentException("Invalid path");return v;}
    static final class GitFailure extends IOException{final String code;GitFailure(String code,String operation){super(code+": "+operation);this.code=code;}}
}
