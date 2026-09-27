package io.github.testlens.migration.tooling;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Safe persistent checkpoint store, separate from source mutation. */
public final class MigrationStateStore {
    private final Path stateRoot;private final MigrationCheckpoint.StateStorage storage;private final MigrationCheckpointJson codec=new MigrationCheckpointJson();
    private MigrationStateStore(Path stateRoot,MigrationCheckpoint.StateStorage storage){this.stateRoot=stateRoot;this.storage=storage;}
    public static MigrationStateStore resolve(MigrationGitService git,MigrationGitService.Result inspected,Path explicitTrustedRoot)throws IOException{
        Objects.requireNonNull(git);Objects.requireNonNull(inspected);if(inspected.localContext()==null)throw new IllegalArgumentException("Detected repository required");Path project=inspected.localContext().worktreeRoot().toRealPath();
        if(explicitTrustedRoot!=null){Path root=validateExplicit(project,inspected.localContext().gitCommonDir().toRealPath(),explicitTrustedRoot);if(root.startsWith(project)&&!git.ignored(project,logical(project,root.resolve("checkpoint.probe"))))throw new IllegalArgumentException("State root inside worktree must already be ignored");return new MigrationStateStore(root,MigrationCheckpoint.StateStorage.EXPLICIT_TRUSTED_ROOT);}
        Path local=project.resolve(Path.of("target","test-lens","migration"));if(git.ignored(project,"target/test-lens/migration/checkpoint.probe"))return new MigrationStateStore(local,MigrationCheckpoint.StateStorage.IGNORED_PROJECT_TARGET);
        String safe=inspected.preflight().repositoryBindingRef().substring(inspected.preflight().repositoryBindingRef().lastIndexOf(':')+1);Path external=Path.of(System.getProperty("user.home"),".test-lens","migration",safe).toAbsolutePath().normalize();return new MigrationStateStore(validateExternal(external),MigrationCheckpoint.StateStorage.EXTERNAL_TOOL_ROOT);
    }
    public MigrationCheckpoint.StateStorage storage(){return storage;}
    public Path write(MigrationCheckpoint checkpoint)throws IOException{Path directory=ensureSafeDirectory(stateRoot.resolve("checkpoints"));String file=checkpoint.checkpointId().replace(':','-')+".json";Path destination=directory.resolve(file);byte[]bytes=codec.write(checkpoint);Path temp=Files.createTempFile(directory,"migration-checkpoint-",".tmp");try{Files.write(temp,bytes);try{Files.move(temp,destination,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temp,destination,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}return destination;}
    public MigrationCheckpoint read(Path checkpointFile)throws IOException{Path file=checkpointFile.toAbsolutePath().normalize();Path realRoot=stateRoot.toAbsolutePath().normalize().toRealPath();if(!file.startsWith(stateRoot.toAbsolutePath().normalize())||Files.isSymbolicLink(file)||!file.toRealPath().startsWith(realRoot))throw new IllegalArgumentException("Checkpoint path escapes state root");byte[]bytes=Files.readAllBytes(file);return codec.read(bytes);}
    Path root(){return stateRoot;}
    private static Path validateExplicit(Path project,Path commonGitDirectory,Path requested)throws IOException{Path root=validateExternal(requested.toAbsolutePath().normalize());if(root.startsWith(project.resolve(".git"))||root.startsWith(commonGitDirectory))throw new IllegalArgumentException("State root cannot be inside Git administrative storage");if(root.startsWith(project)&&!root.startsWith(project.resolve("target")))throw new IllegalArgumentException("State root cannot be inside project source area");return root;}
    private static Path validateExternal(Path root)throws IOException{Path cursor=root;while(cursor!=null&&!Files.exists(cursor,LinkOption.NOFOLLOW_LINKS))cursor=cursor.getParent();if(cursor==null)throw new IOException("State root has no existing ancestor");if(Files.isSymbolicLink(cursor))throw new IllegalArgumentException("State root ancestor is a symbolic link");Path real=cursor.toRealPath();Path rebuilt=real.resolve(cursor.relativize(root));return rebuilt.normalize();}
    private static Path ensureSafeDirectory(Path requested)throws IOException{Path current=requested.getRoot();if(current==null)throw new IllegalArgumentException("Absolute state root required");for(Path part:requested){current=current.resolve(part);if(Files.exists(current,LinkOption.NOFOLLOW_LINKS)){if(Files.isSymbolicLink(current)||!Files.isDirectory(current,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException("Unsafe state-root component");}else Files.createDirectory(current);}return requested.toRealPath();}
    private static String logical(Path root,Path value){return root.relativize(value.normalize()).toString().replace('\\','/');}
}
