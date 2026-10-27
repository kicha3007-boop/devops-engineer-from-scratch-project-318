import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    alias(libs.plugins.versions)
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.spotless)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.lombok)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

// java {
// 	toolchain {
// 		languageVersion = JavaLanguageVersion.of(27)
// 	}
// }

application {
    mainClass.set("io.hexlet.project_devops_deploy.DemoApplication")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterDataJpa)
    implementation(libs.springBootStarterValidation)
    implementation(libs.springBootStarterActuator)
    implementation(libs.springBootStarterFlyway)
    runtimeOnly(libs.flywayPostgresql)
    implementation(libs.mapstruct)
    implementation(libs.instancioCore)
    implementation(libs.springdocOpenapi)
    implementation(libs.micrometerPrometheus)
    developmentOnly(libs.springBootDevtools)
    annotationProcessor(libs.mapstructProcessor)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.lombokMapstructBinding)
    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.lombokMapstructBinding)
    // implementation "org.springframework.boot:spring-boot-starter"
    // testImplementation "org.springframework.boot:spring-boot-starter-test"
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.springBootWebmvcTest)
    testImplementation(libs.springSecurityTest)
    testImplementation(platform(libs.junitBom))
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
    runtimeOnly("com.h2database:h2")
    runtimeOnly(libs.postgresql)

    implementation(libs.jacksonDatabindNullable)
    implementation(libs.datafaker)
    implementation(libs.awsS3)
    implementation(libs.logstashLogbackEncoder)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.SHORT
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
        showStandardStreams = true
    }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        googleJavaFormat().aosp()
        formatAnnotations()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}

// versionCatalogUpdate пишет свежие версии прямо в gradle/libs.versions.toml,
// поэтому руками их сверять не нужно. Ключи не сортируются: порядок в каталоге
// смысловой, по группам зависимостей.
versionCatalogUpdate {
    sortByKey = false
}
