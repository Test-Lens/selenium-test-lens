package io.github.testlens.migration.tooling;
import java.io.IOException;import java.nio.channels.FileChannel;import java.nio.channels.FileLock;import java.nio.channels.OverlappingFileLockException;import java.nio.file.Files;import java.nio.file.Path;import java.nio.file.StandardOpenOption;
/** Operation-scoped cross-process exclusion for verification versus rollback of one transaction. */
final class MigrationTransactionOperationLock implements AutoCloseable{
 private final FileChannel channel;private final FileLock lock;private MigrationTransactionOperationLock(FileChannel c,FileLock l){channel=c;lock=l;}
 static MigrationTransactionOperationLock tryAcquire(MigrationStateStore store,String transaction)throws IOException{Path p=store.root().resolve("apply/transactions/"+transaction.replace(':','-')+"/operation.lock");Files.createDirectories(p.getParent());FileChannel c=FileChannel.open(p,StandardOpenOption.CREATE,StandardOpenOption.WRITE);try{FileLock l=c.tryLock();if(l==null){c.close();return null;}return new MigrationTransactionOperationLock(c,l);}catch(OverlappingFileLockException e){c.close();return null;}}
 public void close()throws IOException{lock.release();channel.close();}
}
