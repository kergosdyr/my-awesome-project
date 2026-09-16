plugins {
    java
    application
    id("com.diffplug.spotless")
}
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
tasks.withType<Test>().configureEach {
    systemProperty("challenge.mysql", project.hasProperty("mysql").toString())
}
tasks.register<Test>("ciTest") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching("challenge.commerce.CommerceInfrastructureTest")
        includeTestsMatching("challenge.commerce.PaymentStateTest")
        includeTestsMatching("challenge.commerce.PaymentNotificationContractTest")
        includeTestsMatching("challenge.commerce.OrderResponseContractTest")
        includeTestsMatching("challenge.commerce.EntityReadBoundaryTest")
    }
}
tasks.register<Test>("priceExperiment") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("challenge.commerce.PriceObservationTest") }
    testLogging.showStandardStreams = true
    outputs.upToDateWhen { false }
}
spotless {
    java {
        target("src/*/java/challenge/commerce/**/*.java")
        palantirJavaFormat("2.96.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
