plugins { java }
repositories { mavenCentral() }
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.testcontainers:testcontainers-mysql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("com.mysql:mysql-connector-j")
}
tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT }
}

tasks.test { useJUnitPlatform { if (project.hasProperty("minimum")) excludeTags("optimization", "concurrency") } }

// MySQL 검증은 서비스 코드 변경 없이 기존 공개 테스트를 그대로 실행한다.
tasks.test { systemProperty("challenge.mysql", project.hasProperty("mysql").toString()) }
tasks.register<JavaExec>("loadServer") {
    group = "verification"
    description = "Start a loopback-only k6 fixture with disposable MySQL"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("loadsupport.LoadTestServer")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
