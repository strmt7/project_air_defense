import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("com.android.application") version "9.2.0" apply false
    id("com.android.test") version "9.2.0" apply false
    kotlin("jvm") version "2.3.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.3" apply false
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
    plugins.withId("com.android.application") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }
    plugins.withId("com.android.test") {
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

    apply(plugin = "dev.detekt")

    plugins.withId("com.android.test") {
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }
    plugins.withId("com.android.application") {
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "com.autonomousapps.dependency-analysis")
    }

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        ignoreFailures = true
        basePath = rootProject.layout.projectDirectory
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
