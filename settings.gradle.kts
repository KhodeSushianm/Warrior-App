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
    }
}

rootProject.name = "Warrior"

include(":app")

include(":feature:auth")
include(":feature:home")
include(":feature:workout")
include(":feature:history")
include(":feature:progress")
include(":feature:profile")

include(":domain:auth")
include(":domain:training")
include(":domain:progress")

include(":data:local")

include(":core:common")
include(":core:designsystem")
include(":core:security")
