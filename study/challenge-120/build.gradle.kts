plugins {
    java
    application
    id("com.diffplug.spotless") version "7.2.1"
}
repositories { mavenCentral() }
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
application { mainClass = "challenge.commerce.CommerceApplication" }

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}

// Past user implementations remain executable, outside the active application classpath.
// These are source sets of this single project, not Gradle subprojects.
val legacy by sourceSets.creating {
    java.srcDir("legacy/main/java")
    resources.srcDir("legacy/main/resources")
}
val legacyTests by sourceSets.creating {
    java.srcDir("legacy/test/java")
    compileClasspath += legacy.output
    runtimeClasspath += legacy.output
}
configurations[legacy.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[legacy.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())
configurations[legacyTests.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[legacyTests.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (project.hasProperty("minimum")) excludeTags("optimization", "concurrency", "full")
        if (!providers.gradleProperty("paymentExtensions").isPresent) excludeTags("extension")
    }
    systemProperty("challenge.mysql", project.hasProperty("mysql").toString())
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
}
val legacyTest by tasks.registering(Test::class) {
    group = "verification"
    testClassesDirs = legacyTests.output.classesDirs
    classpath = legacyTests.runtimeClasspath
}
val commerceInfrastructureTest by tasks.registering(Test::class) {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("challenge.commerce.CommerceInfrastructureTest") }
}
tasks.register<Test>("ciTest") {
    group = "verification"
    dependsOn(legacyTests.classesTaskName, commerceInfrastructureTest)
    testClassesDirs = files(sourceSets.test.get().output.classesDirs, legacyTests.output.classesDirs)
    classpath = sourceSets.test.get().runtimeClasspath + legacyTests.output + legacy.output
    filter {
        includeTestsMatching("challenge.coding.NextHigherTest")
        includeTestsMatching("challenge.payment.PaymentInfrastructureTest")
    }
}
tasks.check { dependsOn(legacyTest) }
tasks.register<JavaExec>("loadServer") {
    group = "verification"
    dependsOn(legacyTests.classesTaskName)
    classpath = legacyTests.runtimeClasspath
    mainClass.set("loadsupport.LoadTestServer")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
tasks.register<JavaExec>("legacyPaymentServer") {
    dependsOn(legacy.classesTaskName)
    classpath = legacy.runtimeClasspath
    mainClass.set("challenge.lab.PaymentLabApplication")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
spotless {
    java {
        target("src/*/java/challenge/commerce/**/*.java", "src/*/java/challenge/coding/FirstUnique*.java", "src/*/java/challenge/coding/SizeSearch*.java")
        palantirJavaFormat("2.96.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Prints the current behavior, before and after the learner's change. Not a completion test.
tasks.register<Test>("priceExperiment") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("challenge.commerce.PriceObservationTest") }
    testLogging.showStandardStreams = true
    outputs.upToDateWhen { false }
}
