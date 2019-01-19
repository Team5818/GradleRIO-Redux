import net.minecrell.gradle.licenser.LicenseExtension

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
    `java-gradle-plugin`
    id("net.researchgate.release") version "2.8.0"
    id("com.techshroom.incise-blue") version "0.3.11"
    id("com.gradle.plugin-publish") version "0.10.0"
}

inciseBlue {
    license()
    util {
        setJavaVersion(JavaVersion.VERSION_11)
    }
    ide()
}
configure<LicenseExtension> {
    include("**/*.kt")
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:3.12.1")
    implementation("edu.wpi.first:GradleRIO:2019.2.1")
    implementation("com.techshroom.incise-blue:com.techshroom.incise-blue.gradle.plugin:0.3.11")
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.12")
}

pluginBundle {
    website = "https://github.com/Team5818/GradleRIO-Redux"
    vcsUrl = "https://github.com/Team5818/GradleRIO-Redux"
    description = "GradleRIO bootstrapper. Biased but simple config."

    plugins {
        create("gradlerioredux") {
            id = "org.rivierarobotics.gradlerioredux"
            displayName = "GradleRIO Redux"
            tags = listOf("FRC", "GradleRIO")
        }
    }
}

tasks.named("afterReleaseBuild") {
    dependsOn("publishPlugins")
}
