package io.github.testlens.migration.tooling;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HexFormat;
import java.util.Set;

/** The sole package-private project-source replacement boundary. */
final class MigrationSourceTransactionWriter {
    interface Faults {
        default void beforeTemp(Path target,int index)throws IOException{}
        default void afterTemp(Path temp,int index)throws IOException{}
        default void beforeMove(Path target,int index)throws IOException{}
        default void afterMove(Path target,int index)throws IOException{}
        Faults NONE=new Faults(){};
    }
    private final Faults faults;
    MigrationSourceTransactionWriter(Faults faults){this.faults=faults==null?Faults.NONE:faults;}
    WriteResult replace(Path target,byte[]bytes,String transaction,String expectedCurrent,int index,
            MigrationApplyPlan.Metadata metadata)throws IOException{
        if(Files.isSymbolicLink(target)||!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))throw new IOException("unsafe/non-regular target");
        String current=sha(Files.readAllBytes(target));if(!current.equals(expectedCurrent))return new WriteResult(false,null,"STALE_PRECONDITION");
        if(metadata.dosReadOnly())return new WriteResult(false,current,"READ_ONLY_SOURCE");
        faults.beforeTemp(target,index);String desired=sha(bytes);Path temp=ownedTemp(target,transaction,desired);
        try(FileChannel channel=FileChannel.open(temp,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
            java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);
        }
        boolean moved=false;try{
            if(!sha(Files.readAllBytes(temp)).equals(desired))throw new IOException("temp digest mismatch");
            setPosix(temp,metadata.posixPermissions());faults.afterTemp(temp,index);faults.beforeMove(target,index);
            try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
            moved=true;setPosix(target,metadata.posixPermissions());faults.afterMove(target,index);
            String observed=sha(Files.readAllBytes(target));return new WriteResult(observed.equals(desired),observed,observed.equals(desired)?null:"VERIFY_DIGEST_FAILED");
        }finally{if(!moved)Files.deleteIfExists(temp);}
    }
    static String sha(byte[]bytes){return"sha256:"+HexFormat.of().formatHex(MigrationDigests.sha256().digest(bytes));}
    static Path ownedTemp(Path target,String transaction,String desired){String token=transaction.substring(transaction.lastIndexOf(':')+1,transaction.lastIndexOf(':')+17);return target.resolveSibling(".test-lens-"+token+"-"+desired.substring(7,23)+".tmp");}
    private static void setPosix(Path path,java.util.List<String>names)throws IOException{var view=Files.getFileAttributeView(path,java.nio.file.attribute.PosixFileAttributeView.class,LinkOption.NOFOLLOW_LINKS);if(view==null||names.isEmpty())return;Set<PosixFilePermission>p=java.util.EnumSet.noneOf(PosixFilePermission.class);names.forEach(x->p.add(PosixFilePermission.valueOf(x)));Files.setPosixFilePermissions(path,p);}
    record WriteResult(boolean written,String observedDigest,String issue){}
}
