import com.techshroom.inciseblue.commonLib
import net.minecrell.gradle.licenser.LicenseExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
    `java-gradle-plugin`
    id("net.researchgate.release") version "2.8.1"
    id("com.techshroom.incise-blue") version "0.5.7"
    id("com.gradle.plugin-publish") version "0.12.0"
}

require(JavaVersion.current().isJava11Compatible) {
    "Java 11+ is needed to build this project."
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
    maven {
        name = "WPI"
        url = uri("https://frcmaven.wpi.edu/artifactory/release")
    }
}

dependencies {
    val wpiVersion = "2021.1.2"
    api(gradleApi())
    // import the linux variant, we just need something to compile against
    compileOnly("edu.wpi.first.tools:PathWeaver:$wpiVersion:linux64")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.guava:guava:30.1-jre")
    implementation("edu.wpi.first:GradleRIO:$wpiVersion")
    implementation("com.techshroom.incise-blue:com.techshroom.incise-blue.gradle.plugin:0.5.7")
    testImplementation(kotlin("test-junit5"))
    commonLib("org.junit.jupiter", "junit-jupiter", "5.7.0") {
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
