

plugins {
    id("java")
    id("fabric-loom") version("1.9.2") apply(false)
}

val MINECRAFT_VERSION by extra { "1.20.1" }
val NEOFORGE_VERSION by extra { "47.3.22" }
val FABRIC_LOADER_VERSION by extra { "0.16.10" }
val FABRIC_API_VERSION by extra { "0.92.3+1.20.1" }

// https://semver.org/
val MOD_VERSION by extra { "0.6.0" }

allprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
    maven("https://maven.tterrag.com/")
    maven("https://maven.blamejared.com")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
    maven("https://maven.covers1624.net/")
}

subprojects {
    apply(plugin = "maven-publish")

    java.toolchain.languageVersion = JavaLanguageVersion.of(17)


    fun createVersionString(): String {
        val builder = StringBuilder()

        val isReleaseBuild = project.hasProperty("build.release")
        val buildId = System.getenv("GITHUB_RUN_NUMBER")

        if (isReleaseBuild) {
            builder.append(MOD_VERSION)
        } else {
            builder.append(MOD_VERSION.substringBefore('-'))
            builder.append("-snapshot")
        }

        builder.append("+mc").append(MINECRAFT_VERSION)

        if (!isReleaseBuild) {
            if (buildId != null) {
                builder.append("-build.${buildId}")
            } else {
                builder.append("-local")
            }
        }

        return builder.toString()
    }

    tasks.processResources {
        filesMatching("META-INF/mods.toml") {
            expand(mapOf("version" to createVersionString()))
        }
    }

    version = createVersionString()
    group = "me.jellysquid.mods"

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    // Disables Gradle's custom module metadata from being published to maven. The
    // metadata includes mapped dependencies which are not reasonably consumable by
    // other mod developers.
    tasks.withType<GenerateModuleMetadata>().configureEach {
        enabled = false
    }
}