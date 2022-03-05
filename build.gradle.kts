import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
    `java-gradle-plugin`
    id("net.researchgate.release") version "2.8.1"
    id("org.cadixdev.licenser") version "0.6.1"
    id("com.gradle.plugin-publish") version "0.19.0"
    `maven-publish`
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

tasks.named<JavaCompile>("compileJava") {
    options.release.set(11)
}

license {
    exclude {
        it.file.startsWith(project.buildDir)
    }
    header(rootProject.file("HEADER.txt"))
    (this as ExtensionAware).extra.apply {
        for (key in listOf("name", "organization", "url")) {
            set(key, rootProject.property(key))
        }
    }
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
    val wpiVersion = "2022.4.1"
    api(gradleApi())
    // import the linux variant, we just need something to compile against
    compileOnly("edu.wpi.first.tools:PathWeaver:$wpiVersion:linux64")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.guava:guava:31.0.1-jre")
    implementation("edu.wpi.first:GradleRIO:$wpiVersion")
    implementation("gradle.plugin.org.cadixdev.gradle:licenser:0.6.1")
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

gradlePlugin {
    plugins {
        create("gradlerioredux") {
            id = "org.rivierarobotics.gradlerioredux"
            implementationClass = "org.rivierarobotics.gradlerioredux.GradleRioRedux"
        }
    }
}

pluginBundle {
    website = "https://github.com/Team5818/GradleRIO-Redux"
    vcsUrl = "https://github.com/Team5818/GradleRIO-Redux"
    description = "GradleRIO bootstrapper. Biased but simple config."

    plugins {
        named("gradlerioredux") {
            displayName = "GradleRIO Redux"
            tags = listOf("FRC", "GradleRIO")
        }
    }
}

tasks.named("afterReleaseBuild") {
    dependsOn("publishPlugins")
}
