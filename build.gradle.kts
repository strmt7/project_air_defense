import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

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
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
    id("com.autonomousapps.dependency-analysis") version "3.7.0" apply false
}

configure<KtlintExtension> {
    version.set("1.8.0")
    ignoreFailures.set(false)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/build/**")
        exclude("**/.gradle/**")
        exclude("**/.kotlin/**")
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.android") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }

    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<KtlintExtension> {
            version.set("1.8.0")
            ignoreFailures.set(false)
            reporters {
                reporter(ReporterType.PLAIN)
                reporter(ReporterType.CHECKSTYLE)
            }
            filter {
                exclude("**/build/**")
                exclude("**/.gradle/**")
                exclude("**/.kotlin/**")
            }
        }
    }

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

tasks.named("ktlintCheck") {
    dependsOn(":android:ktlintCheck", ":benchmarks:ktlintCheck", ":core:ktlintCheck")
}

tasks.named("ktlintFormat") {
    dependsOn(":android:ktlintFormat", ":benchmarks:ktlintFormat", ":core:ktlintFormat")
}

tasks.register("benchmarkStandardsAudit") {
    group = "verification"
    description = "Runs standards-oriented benchmark tasks: detekt, lint, and dependency health."
    dependsOn(
        "ktlintCheck",
        ":core:detekt",
        ":android:detekt",
        ":benchmarks:detekt",
        ":android:lintBenchmark",
        ":core:projectHealth",
        ":android:projectHealth",
        ":benchmarks:projectHealth",
    )
}
