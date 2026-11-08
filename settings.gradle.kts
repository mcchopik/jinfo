pluginManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net") }
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "fabric-loom") {
                useModule("com.fabricmc:fabric-loom:1.10-SNAPSHOT")
            }
        }
    }
}

rootProject.name = "jinfo"