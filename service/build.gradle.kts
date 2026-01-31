// SPDX-FileCopyrightText: 2023-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.flywaydb.flyway") version "11.20.3"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("org.owasp.dependencycheck") version "12.2.0"

    idea
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
}

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

idea {
    module {
        testSources = testSources + sourceSets["e2eTest"].kotlin.sourceDirectories
    }
}

ktlint {
    version.set("1.4.1")
}

dependencies {
    api(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // cve fixes
    api("org.yaml:snakeyaml:2.5")

    // CVE-2025-11226
    api("ch.qos.logback:logback-classic:1.5.26")
    api("ch.qos.logback:logback-core:1.5.26")

    api("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.ws:spring-ws-security") {
        exclude("org.opensaml")
    }

    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql:42.7.9")
    api(platform("org.jdbi:jdbi3-bom:3.51.0"))
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-jackson2")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")

    api(platform("com.fasterxml.jackson:jackson-bom:2.21.0"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("com.auth0:java-jwt:4.5.0")

    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    implementation("ch.qos.logback:logback-access:1.5.26")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    api(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.microsoft.playwright:playwright:1.57.0")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("25")
        allWarningsAsErrors = true
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    outputs.upToDateWhen { false }
}

tasks.getByName<Jar>("jar") {
    archiveClassifier.set("")
}

tasks.getByName<BootJar>("bootJar") {
    archiveClassifier.set("boot")
}

tasks.register("resolveDependencies") {
    description = "Resolves all dependencies"
    doLast {
        configurations
            .matching {
                it.isCanBeResolved &&
                    // ignore configurations that fetch sources (e.g. Java source code)
                    !it.name.endsWith("dependencySources", ignoreCase = true)
            }.map {
                val files = it.resolve()
                it.name to files.size
            }.groupBy({ (_, count) -> count }) { (name, _) -> name }
            .forEach { (count, names) ->
                println(
                    "Resolved $count dependency files for configurations: ${names.joinToString(", ")}"
                )
            }
    }
}

tasks {
    bootRun {
        systemProperty("spring.profiles.active", "local")
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
    }

    dependencyCheck {
        failBuildOnCVSS = 0.0f
        analyzers.apply {
            assemblyEnabled = false
            nodeAuditEnabled = false
            nodeEnabled = false
            nuspecEnabled = false
            ossIndex.apply {
                username = System.getenv("OSS_INDEX_USERNAME")
                password = System.getenv("OSS_INDEX_PASSWORD")
            }
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
