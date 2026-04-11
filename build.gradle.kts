import io.gitlab.arturbosch.detekt.extensions.DetektExtension

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    }
}

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("com.autonomousapps.dependency-analysis") version "3.7.0" apply false
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    plugins.withId("com.android.application") {
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }
    plugins.withId("com.android.test") {
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        ignoreFailures = true
        basePath = rootDir.absolutePath
    }
}

tasks.register("benchmarkStandardsAudit") {
    group = "verification"
    description = "Runs standards-oriented benchmark tasks: detekt, lint, and dependency health."
    dependsOn(
        ":core:detekt",
        ":android:detekt",
        ":benchmarks:detekt",
        ":android:lintBenchmark",
        ":core:projectHealth",
        ":android:projectHealth",
        ":benchmarks:projectHealth"
    )
}
