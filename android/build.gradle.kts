plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStoreFile = providers.gradleProperty("RELEASE_STORE_FILE").orElse(providers.environmentVariable("RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orElse(providers.environmentVariable("RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD"))

android {
    namespace = "com.airdefense.game"
    compileSdk = 36

    signingConfigs {
        create("release") {
            if (
                releaseStoreFile.isPresent &&
                releaseStorePassword.isPresent &&
                releaseKeyAlias.isPresent &&
                releaseKeyPassword.isPresent
            ) {
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
        versionCode = 1
        versionName = "0.1.0"
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val usingReleaseKeystore =
                releaseStoreFile.isPresent &&
                releaseStorePassword.isPresent &&
                releaseKeyAlias.isPresent &&
                releaseKeyPassword.isPresent
            signingConfig = if (usingReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    kotlinOptions {
        jvmTarget = "21"
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

val natives by configurations.creating

dependencies {
    implementation(project(":core"))

    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.5")
    implementation("com.badlogicgames.gdx:gdx:1.13.5")

    val gdxVersion = "1.13.5"
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")

}

tasks.register("printReleaseSigningSource") {
    group = "verification"
    description = "Prints whether release signing uses RELEASE_* properties or debug fallback."
    doLast {
        val usingReleaseKeystore =
            releaseStoreFile.isPresent &&
            releaseStorePassword.isPresent &&
            releaseKeyAlias.isPresent &&
            releaseKeyPassword.isPresent

        if (usingReleaseKeystore) {
            println("[signing] release build will use RELEASE_* properties (store file: ${releaseStoreFile.get()}).")
        } else {
            println("[signing] release build will use the Android debug signing config fallback.")
        }
    }
}

tasks.register("copyNatives") {
    doLast {
        file("libs").deleteRecursively()
        file("libs").mkdirs()
        natives.files.forEach { jar ->
            val abi = when {
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
}
