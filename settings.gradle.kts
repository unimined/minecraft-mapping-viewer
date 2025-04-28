pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

val ktorVersion = settings.ext.get("ktorVersion")
println("from settings.gradle.kts, $ktorVersion")

rootProject.name = "minecraft_mapping_viewer"
