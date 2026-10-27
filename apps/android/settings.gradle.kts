pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "Lughzdan"
include(":app", ":core:game-engine", ":core:database", ":core:datastore", ":core:network", ":core:designsystem", ":domain", ":data", ":feature:games")
