plugins {
    java
    id("com.diffplug.spotless")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        target("src/*/java/challenge/coding/FirstUnique*.java", "src/*/java/challenge/coding/SizeSearch*.java", "src/*/java/challenge/coding/ShippingCapacity*.java", "src/*/java/challenge/coding/LargestReadings*.java")
        palantirJavaFormat("2.96.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Existing C005 gate plus completed C006/C007. All exercise results remain visible via :coding:test.
tasks.register<Test>("ciTest") {
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching("challenge.coding.NextHigherTest")
        includeTestsMatching("challenge.coding.FirstUniqueTest")
        includeTestsMatching("challenge.coding.SizeSearchTest")
    }
}
