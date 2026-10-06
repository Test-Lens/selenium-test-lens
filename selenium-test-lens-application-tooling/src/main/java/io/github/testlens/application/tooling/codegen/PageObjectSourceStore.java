package io.github.testlens.application.tooling.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Writes generated bases atomically and creates user extension classes only when absent. @since 0.5.0 */
public final class PageObjectSourceStore {
    public WriteResult write(PageObjectGenerationOptions options,List<GeneratedPageObject>pages){
        Path root=options.outputDirectory().toAbsolutePath().normalize();Path packageDirectory=root.resolve(options.packageName().replace('.',java.io.File.separatorChar)).normalize();List<Path>generated=new ArrayList<>(),created=new ArrayList<>(),preserved=new ArrayList<>();
        try{requireSafe(root,packageDirectory);Files.createDirectories(packageDirectory);requireSafe(root,packageDirectory);for(GeneratedPageObject page:pages){requireClassName(page.generatedClassName());requireClassName(page.extensionClassName());Path base=packageDirectory.resolve(page.generatedClassName()+".java").normalize();requireSafe(root,base);atomic(base,page.generatedSource());generated.add(base);if(options.createUserExtensions()){Path extension=packageDirectory.resolve(page.extensionClassName()+".java").normalize();requireSafe(root,extension);if(Files.exists(extension,java.nio.file.LinkOption.NOFOLLOW_LINKS))preserved.add(extension);else{atomic(extension,page.extensionSource());created.add(extension);}}}return new WriteResult(generated,created,preserved);}catch(IOException failure){throw new GenerationException("Cannot write generated Page Objects",failure);}
    }
    private static void atomic(Path target,String source)throws IOException{if(Files.isSymbolicLink(target))throw new GenerationException("Generated source target must not be a symbolic link",null);Path temporary=Files.createTempFile(target.getParent(),".test-lens-page-",".tmp");try{Files.writeString(temporary,source,StandardCharsets.UTF_8);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temporary);}}
    private static void requireSafe(Path root,Path target)throws IOException{if(!target.startsWith(root))throw new GenerationException("Generated source path escapes the configured output root",null);if(Files.exists(root,java.nio.file.LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(root))throw new GenerationException("Configured output root must not be a symbolic link",null);Path cursor=root;Path relative=root.relativize(target);for(Path segment:relative){cursor=cursor.resolve(segment);if(Files.exists(cursor,java.nio.file.LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(cursor))throw new GenerationException("Generated source path contains a symbolic link",null);}}
    private static void requireClassName(String value){if(value==null||!value.matches("[A-Za-z_$][A-Za-z0-9_$]*"))throw new GenerationException("Generated class name is unsafe",null);}
    public record WriteResult(List<Path>generatedBases,List<Path>createdExtensions,List<Path>preservedExtensions){public WriteResult{generatedBases=List.copyOf(generatedBases);createdExtensions=List.copyOf(createdExtensions);preservedExtensions=List.copyOf(preservedExtensions);}}
    public static final class GenerationException extends IllegalStateException{public GenerationException(String message,Throwable cause){super(message,cause);}}
}
