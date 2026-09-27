package io.github.testlens.migration.tooling;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;

import static io.github.testlens.migration.tooling.MigrationGitPreflight.*;

final class GitPorcelainV2Parser {
    record Parsed(String branchOid,String branchHead,String upstream,List<StatusEntry>staged,List<StatusEntry>unstaged,
                  List<StatusEntry>untracked,List<StatusEntry>conflicted,List<RenameCopy>renames){ }
    Parsed parse(byte[]bytes){List<String>tokens=statusTokens(bytes);List<StatusEntry>staged=new ArrayList<>(),unstaged=new ArrayList<>(),untracked=new ArrayList<>(),conflicted=new ArrayList<>();List<RenameCopy>renames=new ArrayList<>();String oid=null,head=null,upstream=null;
        for(int i=0;i<tokens.size();i++){String token=tokens.get(i);if(token.isEmpty())continue;if(token.startsWith("# ")){String[]p=token.split(" ",3);if(p.length==3)switch(p[1]){case"branch.oid"->oid=p[2];case"branch.head"->head=p[2];case"branch.upstream"->upstream=p[2];default->{}}continue;}
            char type=token.charAt(0);if(type=='?'){untracked.add(new StatusEntry(token.substring(2),ChangeKind.UNTRACKED,"?","?",null,null));continue;}if(type=='!')continue;
            if(type=='u'){String[]p=token.split(" ",11);if(p.length!=11)throw new Format("Malformed unmerged record");conflicted.add(new StatusEntry(p[10],ChangeKind.UNMERGED,p[1].substring(0,1),p[1].substring(1),p[3],p[7]));continue;}
            if(type!='1'&&type!='2')throw new Format("Unknown porcelain record");String[]p=token.split(" ",type=='1'?9:10);int expected=type=='1'?9:10;if(p.length!=expected)throw new Format("Malformed tracked record");String xy=p[1],path=p[expected-1],old=null;if(type=='2'){if(++i>=tokens.size())throw new Format("Missing rename origin");old=tokens.get(i);}char x=xy.charAt(0),y=xy.charAt(1);ChangeKind xk=kind(x),yk=kind(y);if(x!='.')staged.add(new StatusEntry(path,xk,String.valueOf(x),String.valueOf(y),p[4],p[7]));if(y!='.')unstaged.add(new StatusEntry(path,yk,String.valueOf(x),String.valueOf(y),p[4],p[7]));if(type=='2'){ChangeKind rk=x=='C'?ChangeKind.COPIED:ChangeKind.RENAMED;renames.add(new RenameCopy(path,old,rk));}}
        return new Parsed(oid,head,upstream,List.copyOf(staged),List.copyOf(unstaged),List.copyOf(untracked),List.copyOf(conflicted),List.copyOf(renames));}
    private static ChangeKind kind(char c){return switch(c){case'A'->ChangeKind.ADDED;case'M'->ChangeKind.MODIFIED;case'D'->ChangeKind.DELETED;case'R'->ChangeKind.RENAMED;case'C'->ChangeKind.COPIED;case'T'->ChangeKind.TYPE_CHANGED;case'U'->ChangeKind.UNMERGED;case'?'->ChangeKind.UNTRACKED;default->ChangeKind.UNKNOWN;};}
    static List<String>nul(byte[]bytes){List<String>out=new ArrayList<>();int start=0;for(int i=0;i<bytes.length;i++)if(bytes[i]==0){out.add(decode(bytes,start,i-start));start=i+1;}if(start<bytes.length)out.add(decode(bytes,start,bytes.length-start));return out;}
    private static String decode(byte[] bytes,int offset,int length){try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes,offset,length)).toString();}catch(CharacterCodingException e){throw new Format("Git output is not valid UTF-8");}}
    private static List<String>statusTokens(byte[]bytes){List<String>out=new ArrayList<>();for(String token:nul(bytes)){String remaining=token;while(remaining.startsWith("# ")){int newline=remaining.indexOf('\n');if(newline<0){out.add(remaining);remaining="";break;}out.add(remaining.substring(0,newline));remaining=remaining.substring(newline+1);}if(!remaining.isEmpty())out.add(remaining);}return out;}
    static final class Format extends IllegalArgumentException{Format(String m){super(m);}}
}
