// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.flywaydb.flyway") version "12.6.1"
    id("com.ncorti.ktfmt.gradle") version "0.26.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.owasp.dependencycheck") version "12.2.2"

    idea
}

java { sourceCompatibility = JavaVersion.VERSION_25 }

// CVE-2026-43515, CVE-2026-43512; overrides Spring Boot 4.0.6's tomcat 11.0.21
extra["tomcat.version"] = "11.0.22"

repositories { mavenCentral() }

sourceSets {
    register("e2eTest") {
        compileClasspath += main.get().output + test.get().output
        runtimeClasspath += main.get().output + test.get().output
    }
}

val e2eTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

configurations["e2eTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

idea { module { testSources = testSources + sourceSets["e2eTest"].kotlin.sourceDirectories } }

ktfmt { kotlinLangStyle() }

ktlint { version.set("1.8.0") }

dependencies {
    api(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // cve fixes
    api("org.yaml:snakeyaml:2.6")

    // CVE-2025-11226
    api("ch.qos.logback:logback-classic:1.5.32")
    api("ch.qos.logback:logback-core:1.5.32")

    api("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.ws:spring-ws-security") { exclude("org.opensaml") }

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql:42.7.11")
    api(platform("org.jdbi:jdbi3-bom:3.53.0"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-jackson3")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")

    api(platform("tools.jackson:jackson-bom:3.1.2"))
    implementation("tools.jackson.core:jackson-core")
    implementation("tools.jackson.core:jackson-databind")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("com.auth0:java-jwt:4.5.2")

    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("ch.qos.logback:logback-access:1.5.32")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.03")

    api(platform("io.opentelemetry:opentelemetry-bom:1.62.0"))
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("com.github.kagkarlsson:db-scheduler:16.10.0")
    implementation("org.unbescape:unbescape:1.1.6.RELEASE")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    api(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-property:6.1.11")
    testImplementation("com.microsoft.playwright:playwright:1.60.0")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("25")
        allWarningsAsErrors = true
        freeCompilerArgs =
            listOf(
                // Workaround for a bug that will be fixed in the next Kotlin release
                // https://youtrack.jetbrains.com/issue/KT-78352
                "-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled",

                // This will become the default in the future
                // https://kotlinlang.org/docs/whatsnew2020.html#data-class-copy-function-to-have-the-same-visibility-as-constructor
                "-Xconsistent-data-class-copy-visibility",

                // https://kotlinlang.org/docs/whatsnew22.html#new-defaulting-rules-for-use-site-annotation-targets
                "-Xannotation-default-target=param-property",
            )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    outputs.upToDateWhen { false }
}

tasks.getByName<Jar>("jar") { archiveClassifier.set("") }

tasks.getByName<BootJar>("bootJar") { archiveClassifier.set("boot") }

tasks.register("resolveDependencies") {
    description = "Resolves all dependencies"
    doLast {
        configurations
            .matching {
                it.isCanBeResolved &&
                    // ignore configurations that fetch sources (e.g. Java source code)
                    !it.name.endsWith("dependencySources", ignoreCase = true)
            }
            .map {
                val files = it.resolve()
                it.name to files.size
            }
            .groupBy({ (_, count) -> count }) { (name, _) -> name }
            .forEach { (count, names) ->
                println(
                    "Resolved $count dependency files for configurations: ${names.joinToString(", ")}"
                )
            }
    }
}

tasks {
    test { systemProperty("spring.profiles.active", "test") }

    bootRun { systemProperty("spring.profiles.active", "local") }

    register<KtfmtFormatTask>("ktfmtPrecommit") {
        source = project.fileTree(rootDir)
        include("**/*.kt")
    }

    register("e2eTestDeps", JavaExec::class) {
        group = "build"
        classpath = sourceSets["e2eTest"].runtimeClasspath
        mainClass = "com.microsoft.playwright.CLI"
        args("install-deps")
    }

    register("e2eTest", Test::class) {
        useJUnitPlatform()
        group = "verification"
        testClassesDirs = sourceSets["e2eTest"].output.classesDirs
        classpath = sourceSets["e2eTest"].runtimeClasspath
        shouldRunAfter("test")
        outputs.upToDateWhen { false }
        systemProperty("spring.profiles.active", "e2e")
        testLogging {
            showStandardStreams = true
            events("passed", "skipped", "failed")
        }
    }

    dependencyCheck {
        failBuildOnCVSS = 0.0f
        analyzers.apply {
            assemblyEnabled = false
            nodeAuditEnabled = false
            nodeEnabled = false
            nuspecEnabled = false
            ossIndex.apply { enabled = false }
        }
        nvd.apply { apiKey = System.getenv("NVD_API_KEY") }
        suppressionFile = "$projectDir/owasp-suppressions.xml"
    }
}

flyway {
    url = "jdbc:postgresql://localhost:5432/oppivelvollisuus"
    user = "oppivelvollisuus"
    password = "postgres"
    cleanDisabled = false
}
