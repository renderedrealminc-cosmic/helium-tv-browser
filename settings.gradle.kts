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
        // Mozilla official repository hosting GeckoView
        maven {
            url = java.net.URI("https://maven.mozilla.org/maven2/")
        }
    }
}

rootProject.name = "helium-tv-browser"
include(":app")
