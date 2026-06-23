pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Lokales Maven-Repository (für mandelbrot-core).
        // Pfad-Auflösung in dieser Reihenfolge:
        //   1. Gradle-Property  "mavenLocalRepo"   (-PmavenLocalRepo=... oder in gradle.properties)
        //   2. System-Property  "maven.repo.local" (-Dmaven.repo.local=...)
        //   3. Umgebungsvariable M2_REPO
        //   4. Standard ~/.m2/repository
        val customRepo: String? = (providers.gradleProperty("mavenLocalRepo").orNull
            ?: System.getProperty("maven.repo.local")
            ?: System.getenv("M2_REPO"))
        if (customRepo != null) {
            maven {
                name = "customMavenLocal"
                url = uri(file(customRepo))
            }
        } else {
            mavenLocal()
        }
    }
}

rootProject.name = "mandelbrot-android"

