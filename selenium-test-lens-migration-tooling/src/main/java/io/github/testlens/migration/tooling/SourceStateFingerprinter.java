package io.github.testlens.migration.tooling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static io.github.testlens.migration.tooling.MigrationGitPreflight.*;
import static io.github.testlens.migration.tooling.MigrationSourceStateFingerprint.*;

final class SourceStateFingerprinter {
    private final SafeGit git;
    private final ReadObserver readObserver;
    SourceStateFingerprinter(SafeGit git){this(git,path->{ });}
    SourceStateFingerprinter(SafeGit git,ReadObserver readObserver){this.git=git;this.readObserver=readObserver;}
    MigrationSourceStateFingerprint fingerprint(MigrationGitPreflight preflight,MigrationLocalContext local)throws IOException{
        TreeMap<String,Mutable>dirty=new TreeMap<>();preflight.staged().forEach(e->dirty.computeIfAbsent(e.logicalPath(),Mutable::new).roles.add("STAGED"));preflight.unstaged().forEach(e->dirty.computeIfAbsent(e.logicalPath(),Mutable::new).roles.add("UNSTAGED"));preflight.untracked().forEach(e->dirty.computeIfAbsent(e.logicalPath(),Mutable::new).roles.add("UNTRACKED"));preflight.conflicted().forEach(e->dirty.computeIfAbsent(e.logicalPath(),Mutable::new).roles.add("CONFLICTED"));preflight.renamesAndCopies().forEach(r->dirty.computeIfAbsent(r.logicalPath(),Mutable::new).relations.add(new PathRelation(r.originalLogicalPath(),r.kind())));
        List<String>issues=new ArrayList<>();long[]total={0};MigrationSourceStateFingerprint.Completeness completeness=MigrationSourceStateFingerprint.Completeness.COMPLETE;
        if(dirty.size()>MAX_DIRTY_PATHS){issues.add("DIRTY_PATH_LIMIT_EXCEEDED");completeness=MigrationSourceStateFingerprint.Completeness.INCOMPLETE_SOURCE_STATE;}
        Map<String,List<IndexEntry>>index=index(local.worktreeRoot());
        for(Mutable item:dirty.values().stream().limit(MAX_DIRTY_PATHS).toList()){
            if(item.roles.contains("CONFLICTED")){issues.add("CONFLICT_INDEX_REQUIRES_MANUAL_RESOLUTION:"+item.path);completeness=MigrationSourceStateFingerprint.Completeness.INCOMPLETE_SOURCE_STATE;}
            if(item.roles.contains("STAGED")||item.roles.contains("CONFLICTED")){List<IndexEntry>entries=index.getOrDefault(item.path,List.of());IndexEntry selected=entries.stream().filter(e->e.stage==0).findFirst().orElse(entries.stream().findFirst().orElse(null));if(selected!=null)try{byte[]blob=blob(local.worktreeRoot(),selected.oid,total);item.index=new IndexState(selected.mode,selected.stage,selected.oid,rawSha256(blob),blob.length);}catch(IOException e){issues.add(code(e)+":"+item.path);completeness=MigrationSourceStateFingerprint.Completeness.INCOMPLETE_SOURCE_STATE;}}
            if(item.roles.contains("UNSTAGED")||item.roles.contains("UNTRACKED"))try{item.worktree=worktree(local.worktreeRoot(),item.path,total);}catch(IOException e){issues.add(code(e)+":"+item.path);item.worktree=new WorktreeContent(WorktreeState.UNREADABLE,null,0,null);completeness=MigrationSourceStateFingerprint.Completeness.INCOMPLETE_SOURCE_STATE;}
            else item.worktree=new WorktreeContent(WorktreeState.NOT_APPLICABLE,null,0,null);
        }
        List<FileState>files=dirty.values().stream().limit(MAX_DIRTY_PATHS).map(Mutable::freeze).toList();String digest=digest(preflight.head().tree(),files);return new MigrationSourceStateFingerprint(1,"migration-source-state-v1:sha256:"+digest,preflight.head().tree(),files,total[0],completeness,issues);
    }
    private Map<String,List<IndexEntry>>index(Path root)throws IOException{GitProcessRunner.Result r=git.execute(SafeGit.Operation.INDEX,root,null);SafeGit.requireComplete(r,"INDEX");Map<String,List<IndexEntry>>out=new HashMap<>();for(String token:GitPorcelainV2Parser.nul(r.stdout())){if(token.isEmpty())continue;int tab=token.indexOf('\t');if(tab<0)throw new IOException("Malformed ls-files record");String[]meta=token.substring(0,tab).split(" ");if(meta.length!=3)throw new IOException("Malformed ls-files metadata");String path=MigrationGitPreflight.path(token.substring(tab+1));out.computeIfAbsent(path,k->new ArrayList<>()).add(new IndexEntry(meta[0],meta[1],Integer.parseInt(meta[2])));}return out;}
    private byte[]blob(Path root,String oid,long[]total)throws IOException{GitProcessRunner.Result r=git.execute(SafeGit.Operation.READ_BLOB,root,oid);SafeGit.requireComplete(r,"READ_BLOB");if(r.stdout().length>MAX_FILE_BYTES)throw new Limit("FILE_HASH_LIMIT_EXCEEDED");if(total[0]+r.stdout().length>MAX_TOTAL_BYTES)throw new Limit("AGGREGATE_HASH_LIMIT_EXCEEDED");total[0]+=r.stdout().length;return r.stdout();}
    private WorktreeContent worktree(Path root,String logical,long[]total)throws IOException{Path path=resolve(root,logical);if(!Files.exists(path,LinkOption.NOFOLLOW_LINKS))return new WorktreeContent(WorktreeState.DELETED,null,0,null);if(Files.isSymbolicLink(path)){String target=Files.readSymbolicLink(path).toString();long size=target.getBytes(StandardCharsets.UTF_8).length;if(size>MAX_FILE_BYTES)throw new Limit("FILE_HASH_LIMIT_EXCEEDED");if(total[0]+size>MAX_TOTAL_BYTES)throw new Limit("AGGREGATE_HASH_LIMIT_EXCEEDED");total[0]+=size;return new WorktreeContent(WorktreeState.SYMLINK,null,size,MigrationDigests.digest("migration-symlink-target-v1",target));}if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))return new WorktreeContent(WorktreeState.OTHER,null,0,null);
        BasicFileAttributes before=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);if(before.size()>MAX_FILE_BYTES)throw new Limit("FILE_HASH_LIMIT_EXCEEDED");if(total[0]+before.size()>MAX_TOTAL_BYTES)throw new Limit("AGGREGATE_HASH_LIMIT_EXCEEDED");readObserver.afterInitialMetadata(path);MessageDigest md=MigrationDigests.sha256();long count=0;try(SeekableByteChannel channel=Files.newByteChannel(path,Set.of(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS))){ByteBuffer buffer=ByteBuffer.allocate(8192);for(int read;(read=channel.read(buffer))!=-1;){if(read==0)continue;buffer.flip();count+=read;if(count>MAX_FILE_BYTES||total[0]+count>MAX_TOTAL_BYTES)throw new Limit("HASH_LIMIT_EXCEEDED");md.update(buffer);buffer.clear();}}BasicFileAttributes after=Files.readAttributes(path,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);if(before.size()!=after.size()||!before.lastModifiedTime().equals(after.lastModifiedTime())||!java.util.Objects.equals(before.fileKey(),after.fileKey()))throw new Limit("SOURCE_CHANGED_DURING_CHECKPOINT");total[0]+=count;return new WorktreeContent(WorktreeState.REGULAR_FILE,HexFormat.of().formatHex(md.digest()),count,null);}
    private static Path resolve(Path root,String logical)throws IOException{Path normalized=root.resolve(logical.replace('/',java.io.File.separatorChar)).normalize();if(!normalized.startsWith(root)||normalized.startsWith(root.resolve(".git")))throw new IOException("Source path escapes worktree");Path parent=normalized.getParent();if(parent!=null&&Files.exists(parent,LinkOption.NOFOLLOW_LINKS)&&!parent.toRealPath().startsWith(root.toRealPath()))throw new IOException("Source parent escapes through symlink");return normalized;}
    private static String digest(String tree,List<FileState>files){List<String>fields=new ArrayList<>();fields.add(tree==null?"UNBORN":tree);for(FileState f:files){fields.add("FILE");fields.add(f.logicalPath());fields.add(Integer.toString(f.roles().size()));f.roles().stream().sorted().forEach(fields::add);fields.add(Integer.toString(f.relations().size()));for(PathRelation relation:f.relations()){fields.add(relation.kind().name());fields.add(relation.originalLogicalPath());}IndexState i=f.index();fields.add(i==null?"NO_INDEX":"INDEX");if(i!=null){fields.add(i.mode());fields.add(Integer.toString(i.stage()));fields.add(i.objectId());fields.add(i.sha256());fields.add(Long.toString(i.size()));}WorktreeContent w=f.worktree();fields.add(w==null?"NO_WORKTREE":"WORKTREE");if(w!=null){fields.add(w.state().name());fields.add(n(w.sha256()));fields.add(Long.toString(w.size()));fields.add(n(w.linkTargetDigest()));}}return MigrationDigests.digest("migration-source-state-v1",fields.toArray(String[]::new));}
    private static String n(String value){return value==null?"":value;}
    private static String code(IOException error){return error instanceof Limit limit?limit.code:"SOURCE_READ_FAILED";}
    private static String rawSha256(byte[]bytes){return HexFormat.of().formatHex(MigrationDigests.sha256().digest(bytes));}
    private record IndexEntry(String mode,String oid,int stage){}
    private static final class Mutable{final String path;final Set<String>roles=new LinkedHashSet<>();final List<PathRelation>relations=new ArrayList<>();IndexState index;WorktreeContent worktree;Mutable(String p){path=p;}FileState freeze(){return new FileState(path,roles,relations,index,worktree);}}
    private static final class Limit extends IOException{final String code;Limit(String code){super(code);this.code=code;}}
    @FunctionalInterface interface ReadObserver{void afterInitialMetadata(Path path)throws IOException;}
}
