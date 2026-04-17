import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    id("com.gradleup.shadow") version "9.4.1"
}

group = "cn.cctstudio"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0")

    implementation("com.github.retrooper:packetevents-velocity:2.12.0")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.processResources {
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filesMatching("velocity-plugin.json") {
        expand(properties)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("CCTPickBlockFix")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    relocate(
        "com.github.retrooper",
        "cn.cctstudio.velocity.pickblockfix.libs.packetevents.api"
    )
    relocate(
        "io.github.retrooper",
        "cn.cctstudio.velocity.pickblockfix.libs.packetevents.impl"
    )

    dependencies {
        exclude(dependency("com.velocitypowered:velocity-api:.*"))
        exclude(dependency("io.netty:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
