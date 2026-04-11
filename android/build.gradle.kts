import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE").orElse(providers.environmentVariable("RELEASE_STORE_FILE"))
val releaseStorePassword =
    providers
        .gradleProperty(
            "RELEASE_STORE_PASSWORD",
        ).orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orElse(providers.environmentVariable("RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD"))
val explicitVersionCode = providers.gradleProperty("APP_VERSION_CODE").orElse(providers.environmentVariable("APP_VERSION_CODE"))
val explicitVersionName = providers.gradleProperty("APP_VERSION_NAME").orElse(providers.environmentVariable("APP_VERSION_NAME"))
val gitExecutable =
    sequenceOf(
        "git",
        "C:/Program Files/Git/cmd/git.exe",
        "C:/Program Files/Git/bin/git.exe",
    ).firstOrNull { candidate ->
        candidate == "git" || File(candidate).exists()
    }

fun resolveVersionCodeFromGit(
    gitExecutable: String,
    workingDir: File,
    fallbackVersionCode: String,
): String =
    try {
        val process =
            ProcessBuilder(gitExecutable, "rev-list", "--count", "HEAD")
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (process.waitFor() == 0) output.ifBlank { fallbackVersionCode } else fallbackVersionCode
    } catch (_: Exception) {
        fallbackVersionCode
    }

val gitVersionCodeProvider =
    providers.provider {
        val gitDir = rootProject.file(".git")
        val fallbackVersionCode = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        if (!gitDir.exists() || gitExecutable == null) return@provider fallbackVersionCode
        resolveVersionCodeFromGit(gitExecutable, rootProject.rootDir, fallbackVersionCode)
    }
val computedVersionCode = explicitVersionCode.orElse(gitVersionCodeProvider).map { it.toInt() }
val computedVersionName = explicitVersionName.orElse(computedVersionCode.map { "0.1.$it" })
val hasReleaseSigning =
    releaseStoreFile.isPresent && releaseStorePassword.isPresent && releaseKeyAlias.isPresent && releaseKeyPassword.isPresent

android {
    namespace = "com.airdefense.game"
    compileSdk = 36

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    defaultConfig {
        applicationId = "com.airdefense.game"
        minSdk = 28
        targetSdk = 36
        versionCode = computedVersionCode.get()
        versionName = computedVersionName.get()
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }

        create("local") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            matchingFallbacks += listOf("debug")
        }

        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            versionNameSuffix = "-benchmark"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

val natives by configurations.creating

dependencies {
    implementation(project(":core"))

    val gdxVersion = "1.14.0"
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    runtimeOnly("androidx.profileinstaller:profileinstaller:1.4.1")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
}

tasks.register("printReleaseSigningSource") {
    group = "verification"
    description = "Prints whether release signing uses RELEASE_* properties."
    doLast {
        if (hasReleaseSigning) {
            println("[signing] release build will use RELEASE_* properties (store file: ${releaseStoreFile.get()}).")
        } else {
            println("[signing] release build is disabled until RELEASE_* properties are configured.")
        }
    }
}

tasks.register("printAppIdentity") {
    group = "verification"
    description = "Prints package ids and version metadata for debug/local/release channels."
    doLast {
        println(
            "[identity] release package=com.airdefense.game versionCode=${computedVersionCode.get()} versionName=${computedVersionName.get()}",
        )
        println(
            "[identity] benchmark package=com.airdefense.game.benchmark versionCode=${computedVersionCode.get()} versionName=${computedVersionName.get()}-benchmark",
        )
        println(
            "[identity] local package=com.airdefense.game.local versionCode=${computedVersionCode.get()} versionName=${computedVersionName.get()}-local",
        )
        println(
            "[identity] debug package=com.airdefense.game.debug versionCode=${computedVersionCode.get()} versionName=${computedVersionName.get()}-debug",
        )
    }
}

tasks.register("copyNatives") {
    doLast {
        file("libs").deleteRecursively()
        file("libs").mkdirs()
        natives.files.forEach { jar ->
            val abi =
                when {
                    jar.name.contains("arm64-v8a") -> "arm64-v8a"
                    jar.name.contains("armeabi-v7a") -> "armeabi-v7a"
                    jar.name.contains("x86_64") -> "x86_64"
                    jar.name.contains("x86") -> "x86"
                    else -> null
                }
            if (abi != null) {
                copy {
                    from(zipTree(jar))
                    into("libs/$abi")
                    include("*.so")
                }
            }
        }
    }
}

tasks.configureEach {
    if (name.contains("package")) {
        dependsOn("copyNatives")
    }
    if (name == "assembleRelease" || name == "bundleRelease") {
        onlyIf("release signing must be configured for update-safe release artifacts") {
            hasReleaseSigning
        }
    }
}
