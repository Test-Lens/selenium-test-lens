package io.github.testlens.migration.tooling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Fixed-main-class Java adapter for trusted Gradle/Maven wrapper layouts; never invokes a shell. */
public final class MigrationJavaWrapperMain {
    public MigrationVerificationPlan.WrapperBinding inspect(Path projectRoot,MigrationVerificationPlan.WrapperBinding.WrapperKind kind,String javaExecutableRef)throws IOException{
        Path root=projectRoot.toRealPath();String jar=kind==MigrationVerificationPlan.WrapperBinding.WrapperKind.GRADLE_WRAPPER_MAIN?"gradle/wrapper/gradle-wrapper.jar":".mvn/wrapper/maven-wrapper.jar";String properties=kind==MigrationVerificationPlan.WrapperBinding.WrapperKind.GRADLE_WRAPPER_MAIN?"gradle/wrapper/gradle-wrapper.properties":".mvn/wrapper/maven-wrapper.properties";Path j=safe(root,jar),p=safe(root,properties);if(!Files.isRegularFile(j,LinkOption.NOFOLLOW_LINKS)||!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new UnsupportedLayout("UNSUPPORTED_WRAPPER_LAYOUT");String jd="sha256:"+MigrationDigests.rawBytes(Files.readAllBytes(j)),pd="sha256:"+MigrationDigests.rawBytes(Files.readAllBytes(p));String ref=MigrationVerificationPlan.ref("migration-wrapper-binding-v1",kind.name(),javaExecutableRef,jar,jd,properties,pd);return new MigrationVerificationPlan.WrapperBinding(ref,kind,javaExecutableRef,jar,jd,properties,pd);
    }
    public List<String> commandArguments(Path projectRoot,MigrationVerificationPlan.WrapperBinding binding,List<String>buildArguments)throws IOException{
        revalidate(projectRoot,binding);List<String>out=new ArrayList<>();out.add("-classpath");out.add(safe(projectRoot.toRealPath(),binding.jarLogicalPath()).toString());out.add(binding.kind()==MigrationVerificationPlan.WrapperBinding.WrapperKind.GRADLE_WRAPPER_MAIN?"org.gradle.wrapper.GradleWrapperMain":"org.apache.maven.wrapper.MavenWrapperMain");if(buildArguments!=null)out.addAll(buildArguments);return List.copyOf(out);
    }
    public void revalidate(Path projectRoot,MigrationVerificationPlan.WrapperBinding binding)throws IOException{
        Path root=projectRoot.toRealPath(),jar=safe(root,binding.jarLogicalPath()),properties=safe(root,binding.propertiesLogicalPath());if(!digest(jar).equals(binding.jarSha256()))throw new StaleWrapper("WRAPPER_JAR_CHANGED");if(!digest(properties).equals(binding.propertiesSha256()))throw new StaleWrapper("WRAPPER_PROPERTIES_CHANGED");
    }
    public static String sanitizedDistributionUrl(byte[]properties){String text=new String(properties,java.nio.charset.StandardCharsets.ISO_8859_1);for(String line:text.split("\\R"))if(line.stripLeading().startsWith("distributionUrl=")){String value=line.substring(line.indexOf('=')+1).replace("\\:",":");try{java.net.URI uri=java.net.URI.create(value);return new java.net.URI(uri.getScheme(),null,uri.getHost(),uri.getPort(),uri.getPath(),null,null).toString();}catch(Exception ignored){return "[REDACTED_DISTRIBUTION_URL]";}}return "";}
    private static String digest(Path path)throws IOException{if(Files.isSymbolicLink(path)||!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new UnsupportedLayout("UNSAFE_WRAPPER_PATH");return"sha256:"+MigrationDigests.rawBytes(Files.readAllBytes(path));}
    private static Path safe(Path root,String logical)throws IOException{String l=MigrationRunPlan.logical(logical);Path p=root.resolve(l.replace('/',java.io.File.separatorChar)).normalize();if(!p.startsWith(root))throw new IllegalArgumentException("wrapper path escape");Path cursor=p.getParent();while(cursor!=null&&!Files.exists(cursor,LinkOption.NOFOLLOW_LINKS))cursor=cursor.getParent();if(cursor==null||Files.isSymbolicLink(cursor)||!cursor.toRealPath().startsWith(root))throw new IllegalArgumentException("wrapper symlink escape");return p;}
    public static final class UnsupportedLayout extends IOException{public UnsupportedLayout(String message){super(message);}}
    public static final class StaleWrapper extends IOException{public StaleWrapper(String message){super(message);}}
}
