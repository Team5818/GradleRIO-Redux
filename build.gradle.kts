import com.techshroom.inciseblue.commonLib
import net.minecrell.gradle.licenser.LicenseExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
    `java-gradle-plugin`
    id("net.researchgate.release") version "2.8.0"
    id("com.techshroom.incise-blue") version "0.3.13"
    id("com.gradle.plugin-publish") version "0.10.1"
}

inciseBlue {
    license()
    util {
        setJavaVersion(JavaVersion.VERSION_11)
        enableJUnit5()
    }
    ide()
}
configure<LicenseExtension> {
    include("**/*.kt")
}

// Extra test logging for CI
when (System.getenv("CI")) {
    "true", "1" ->
        tasks.withType<Test>().configureEach {
            this.testLogging {
                events("passed", "skipped", "failed")
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.squareup.okhttp3:okhttp:3.13.1")
    implementation("edu.wpi.first:GradleRIO:2019.3.2")
    implementation("com.techshroom.incise-blue:com.techshroom.incise-blue.gradle.plugin:0.3.13")
    testImplementation(kotlin("test-junit5"))
    commonLib("org.junit.jupiter", "junit-jupiter", "5.4.0") {
        testImplementation(lib("api"))
        testImplementation(lib("engine"))
        testImplementation(lib("params"))
    }
    testImplementation(gradleTestKit())
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
