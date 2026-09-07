import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

plugins {
    java
}

val lensVersion = providers.gradleProperty("testLensVersion").get()
val isolatedLensRepository = file(providers.gradleProperty("testLensRepository").get()).canonicalFile

group = "io.github.testlens.consumer"
version = "1.0.0"

dependencies {
    testImplementation("io.github.test-lens:selenium-test-lens:$lensVersion")
    testImplementation("io.github.test-lens:selenium-test-lens-react:$lensVersion")
    testImplementation("io.github.test-lens:selenium-test-lens-junit5:$lensVersion")
    testImplementation("io.github.test-lens:selenium-test-lens-testng:$lensVersion")
    testImplementation("org.seleniumhq.selenium:selenium-java:4.39.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

configurations.configureEach {
    resolutionStrategy.failOnVersionConflict()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

val verifyResolvedGraph = tasks.register("verifyResolvedGraph") {
    val reportFile = layout.buildDirectory.file("reports/dependency-graph.txt")
    outputs.file(reportFile)
    doLast {
        val runtime = configurations.testRuntimeClasspath.get()
        val declared = configurations.flatMap { it.dependencies }.distinct()
        check(declared.none { it is ProjectDependency }) { "Project dependencies are forbidden" }
        check(declared.filterIsInstance<ExternalModuleDependency>().none {
            val value = it.version.orEmpty()
            value.contains('+') || value.startsWith("latest.") || value.contains('[') || value.contains('(')
        }) { "Dynamic dependency versions are forbidden" }

        val unresolved = runtime.resolvedConfiguration.lenientConfiguration.unresolvedModuleDependencies
        check(unresolved.isEmpty()) {
            "Unresolved dependencies: " + unresolved.joinToString { it.selector.toString() }
        }

        val components = runtime.incoming.resolutionResult.allComponents
        val projectComponents = components.filter { it.id is ProjectComponentIdentifier }
        check(projectComponents.size == 1 &&
            (projectComponents.single().id as ProjectComponentIdentifier).projectPath == ":") {
            "Resolved project dependency is forbidden: ${projectComponents.map { it.id }}"
        }
        check(components.none { it.selectionReason.isSelectedByRule }) { "Dependency substitution or selection rules are forbidden" }

        val modules = components.mapNotNull { it.id as? ModuleComponentIdentifier }
        val lensModules = modules.filter { it.group == "io.github.test-lens" }
        val requiredLensModules = setOf(
            "selenium-test-lens", "selenium-test-lens-core", "selenium-test-lens-overlay",
            "selenium-test-lens-react", "selenium-test-lens-junit5", "selenium-test-lens-testng"
        )
        check(lensModules.map { it.module }.containsAll(requiredLensModules)) {
            "Missing Test Lens modules: " + (requiredLensModules - lensModules.map { it.module }.toSet())
        }
        check(lensModules.all { it.version == lensVersion && !it.version.endsWith("-SNAPSHOT") }) {
            "Every Test Lens module must resolve as release $lensVersion: $lensModules"
        }
        val isolatedRepositoryPath = isolatedLensRepository.toPath()
        val lensArtifacts = runtime.resolvedConfiguration.resolvedArtifacts.filter {
            it.moduleVersion.id.group == "io.github.test-lens"
        }
        check(lensArtifacts.all { it.file.canonicalFile.toPath().startsWith(isolatedRepositoryPath) }) {
            "A Test Lens artifact was resolved outside the isolated repository: " +
                lensArtifacts.filterNot { it.file.canonicalFile.toPath().startsWith(isolatedRepositoryPath) }
                    .joinToString { it.file.toString() }
        }

        val seleniumVersions = modules.filter { it.group == "org.seleniumhq.selenium" }.map { it.version }.toSet()
        check(seleniumVersions == setOf("4.39.0")) { "Multiple or unexpected Selenium versions: $seleniumVersions" }
        check(declared.none { it.group == "org.testng" }) { "TestNG must be supplied by the TestNG adapter" }
        check(declared.filter { it.group?.startsWith("org.junit") == true }.all {
            it.name == "junit-jupiter-engine" || it.name == "junit-platform-launcher"
        }) { "JUnit may be declared only for the consumer test engine/launcher" }

        val lines = modules
            .map { "${it.group}:${it.module}:${it.version}" }
            .distinct()
            .sorted()
        val report = buildString {
            appendLine("testLensVersion=$lensVersion")
            appendLine("javaRuntimeVersion=${System.getProperty("java.version")}")
            appendLine("javaClassVersion=${System.getProperty("java.class.version")}")
            appendLine("testLensOrigin=exclusive isolated Maven repository")
            appendLine("projectDependencies=false")
            appendLine("dependencySubstitution=false")
            lines.forEach(::appendLine)
        }
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(report)
        }
        println(report)
    }
}

tasks.test {
    dependsOn(verifyResolvedGraph)
    useJUnitPlatform()
    reports.junitXml.required.set(true)
    reports.html.required.set(true)
}
