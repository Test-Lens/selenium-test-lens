import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "selenium-test-lens-gradle-consumer"

val lensVersion = providers.gradleProperty("testLensVersion").orNull
    ?: throw GradleException("Required property 'testLensVersion' is missing")
if (lensVersion.endsWith("-SNAPSHOT")) {
    throw GradleException("testLensVersion must be a release version, not '$lensVersion'")
}

val repositoryValue = providers.gradleProperty("testLensRepository").orNull
    ?: throw GradleException("Required property 'testLensRepository' is missing")
val lensRepository = file(repositoryValue).canonicalFile
if (!lensRepository.isDirectory) {
    throw GradleException("testLensRepository does not exist or is not a directory")
}
val sourceRootValue = providers.gradleProperty("testLensSourceRoot").orNull
    ?: throw GradleException("Required property 'testLensSourceRoot' is missing")
val sourceRoot = file(sourceRootValue).canonicalFile
if (!sourceRoot.isDirectory) {
    throw GradleException("testLensSourceRoot does not exist or is not a directory")
}
if (lensRepository.toPath().startsWith(sourceRoot.toPath())) {
    throw GradleException("testLensRepository must be isolated from the project source tree")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "isolatedTestLensRelease"
                    url = uri(lensRepository)
                    metadataSources { mavenPom(); artifact() }
                }
            }
            filter { includeGroup("io.github.test-lens") }
        }
        mavenCentral()
    }
}
