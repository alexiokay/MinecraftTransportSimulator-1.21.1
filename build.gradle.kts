import java.nio.file.Paths
import kotlin.io.path.moveTo
import kotlin.io.path.ExperimentalPathApi


plugins {
    java
    kotlin("jvm") version "1.7.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

subprojects {
    apply(plugin = "java")
}

var modVersion: String = project.property("global_version").toString()

//var modVersion: String = providers.gradleProperty("global_version")

var mcCore = project(":mccore")

var mcInterfaceNeoForge1211 = project(":neoforge")

tasks.register("buildCore") {
    dependsOn(mcCore.tasks.build)
    doLast {
        moveToOut(mcCore, "core")
    }
}

/*
tasks.register("buildForge1122") {
    doFirst { preBuild() }
    doLast {
        moveToOut(mcInterfaceForge1122, "1.12.2")
    }
    dependsOn(mcInterfaceForge1122.tasks.build)
}
*/

tasks.register("buildNeoForge1211") {
    doFirst { preBuild() }
    doLast {
        moveToOut(mcInterfaceNeoForge1211, "1.21.1")
    }
    dependsOn(mcInterfaceNeoForge1211.tasks.build)
}

tasks.register("buildForgeAll") {
    dependsOn(tasks.getByName("buildForge1122"))

}

@OptIn(ExperimentalPathApi::class)
fun moveToOut(subProject: Project, versionStr: String) {
    val jarName = "Immersive Vehicles-${subProject.version}.jar"
    Paths.get("${subProject.projectDir.canonicalPath}/build/libs/$jarName")
        .moveTo(Paths.get("${project.projectDir.canonicalPath}/out/$jarName"), true)
}

fun preBuild() {
    // Could probably be better somehow, but I'm not sure how
    project.projectDir.canonicalFile.walk()
        .filter { it.name == "gradle.properties" || it.name == "mcmod.info" || it.name == "InterfaceLoader.java" }
        .forEach { it.writeText(it.readText()
            .replace(Regex("mod_version=(.+)"), "mod_version=$modVersion")
            .replace(Regex("\"version\": \"[^\"]*\""), "\"version\": \"$modVersion\"")
            .replace(Regex("MODVER = \"[^\"]*\";"), "MODVER = \"$modVersion\";")) }
}