plugins {
    base
    id("com.diffplug.spotless") version "7.2.1" apply false
}

subprojects {
    apply(plugin = "java")
    repositories { mavenCentral() }
    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform {
            if (project.hasProperty("minimum")) excludeTags("optimization", "concurrency", "full")
        }
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
        }
    }
}

tasks.register("ciTest") {
    group = "verification"
    dependsOn(":coding:ciTest", ":commerce:ciTest")
}
tasks.register("test") {
    group = "verification"
    dependsOn(":coding:test", ":commerce:test")
}
tasks.named("check") { dependsOn("ciTest") }
tasks.named("assemble") { dependsOn(":coding:assemble", ":commerce:assemble") }
tasks.named("build") { dependsOn(":coding:build", ":commerce:build") }
tasks.register("spotlessApply") { dependsOn(":coding:spotlessApply", ":commerce:spotlessApply") }
tasks.register("spotlessCheck") { dependsOn(":coding:spotlessCheck", ":commerce:spotlessCheck") }
