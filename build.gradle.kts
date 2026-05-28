plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.2"
}

group = "org.avarion"
description = "Full featured lightweight death chest plugin."
version = (findProperty("version") as? String)?.takeIf { it != "unspecified" } ?: "0.0.0-SNAPSHOT"

val displayName = "Graves"
val testServerPath: String = (findProperty("test.server.path") as? String)
    ?: file("../server").absolutePath

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf(
        "-Xlint:deprecation",
        "-Xlint:unchecked",
        "-Xlint:-options",
        "-Werror",
    ))
}

// Several `compileOnly` server-side deps (WorldEdit 7.4.3, WorldGuard 7.0.16,
// FurnitureLib v3.2.8) declare a newer JVM floor in their Gradle module
// metadata. Our plugin still emits Java 17 bytecode, but those libs are only
// loaded at runtime on the user's server, which by then is running a JVM
// recent enough to load them. Tell Gradle's variant resolution to accept
// them on the compile classpath.
configurations.compileClasspath.configure {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
    // Log4j is provided by the Minecraft server (Mojang ships it). WorldEdit
    // and WorldGuard each pin different *strict* log4j-bom versions in their
    // module metadata, which Gradle refuses to resolve. Drop it from the
    // compile classpath — the server has its own copy at runtime.
    exclude(group = "org.apache.logging.log4j")
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://libraries.minecraft.net/")
    maven("https://repo.clojars.org/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.skriptlang.org/releases")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.glaremasters.me/repository/towny/")
    maven("https://repo.minebench.de/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.oraxen.com/releases/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://maven.playpro.com")
    maven {
        url = uri(layout.projectDirectory.dir("lib"))
        metadataSources {
            mavenPom()
            artifact()
        }
    }
    maven("https://repo.bluecolored.de/releases")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("com.github.puregero:multilib:1.2.5")
    compileOnly("com.mojang:authlib:1.5.25")
    compileOnly("dev.sergiferry:playernpc:2022.9")
    compileOnly("com.mira:furnitureengine:1.6.3")
    compileOnly("net.milkbowl:vault:1.7.3")
    compileOnly("me.lokka30:treasury:1.2.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.3")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.16")
    compileOnly("com.palmergames.bukkit.towny:towny:0.103.0.1")
    compileOnly("de.jeff_media:ChestSortAPI:13.0.0-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.2")
    compileOnly("com.github.SkriptLang:Skript:2.16.0-feature-docs-overhaul")
    compileOnly("com.github.xerial:sqlite-jdbc:3.45.2.0")
    compileOnly("com.github.Ste3et:FurnitureLib:3.2.8")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("io.th0rgal:oraxen:1.213.0") {
        exclude(group = "me.gabytm.util", module = "actions-spigot")
        exclude(group = "com.ticxo", module = "PlayerAnimator")
    }
    compileOnly("com.github.jojodmo:ItemBridge:b0054538c1")
    compileOnly("net.kyori:adventure-text-minimessage:4.26.1")
    compileOnly("net.kyori:adventure-platform-bukkit:4.4.1")
    compileOnly("de.themoep:minedown:1.7.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.1.0")
    implementation("de.tr7zw:item-nbt-api-plugin:2.15.7")
    compileOnly("net.coreprotect:coreprotect:23.2")
    compileOnly("com.github.Xyness:SimpleClaimSystem:1.13.0.8")
}

tasks.processResources {
    val projectInfo = mapOf(
        "name" to displayName,
        "description" to (project.description ?: ""),
        "version" to project.version.toString(),
    )
    inputs.properties(projectInfo)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(mapOf("project" to projectInfo))
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set(displayName)
    archiveClassifier.set("")
    relocate("org.bstats", "${project.group}.${project.name}.bstats")
    relocate("com.github.puregero.multilib", "${project.group}.${project.name}.multilib")
    relocate("de.tr7zw.changeme.nbtapi", "${project.group}.${project.name}.nbtapi")
    exclude("META-INF/**")
    minimize()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.register<Copy>("copyToTestServer") {
    description = "Copies the shaded plugin jar into the configured test server's plugins directory."
    group = "distribution"
    from(tasks.shadowJar)
    into("$testServerPath/plugins")
}

tasks.shadowJar {
    finalizedBy("copyToTestServer")
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks.shadowJar)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AvarionMC/graves")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
