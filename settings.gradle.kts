pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://stream-io-repo.com") }
        maven { url = java.net.URI("https://jitpack.io") }
    }
}

rootProject.name = "CampusNetwork"

// Multi-module layout following ComposeTemplate architectural conventions
include(":app")
include(":core:model")
include(":core:security")
include(":core:database")
include(":core:network")
include(":core:data")
include(":core:ui")
include(":feature:map")
include(":feature:channels")
include(":feature:messages")
include(":feature:hub")
include(":feature:profile")
