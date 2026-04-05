plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.airdefense.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.airdefense.game"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    implementation(project(":core"))

    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.5")
    implementation("com.badlogicgames.gdx:gdx:1.13.5")
    implementation("com.badlogicgames.gdx:gdx-bullet:1.13.5")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.5:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.5:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-platform:1.13.5:natives-x86_64")

    implementation("com.badlogicgames.gdx:gdx-bullet-platform:1.13.5:natives-armeabi-v7a")
    implementation("com.badlogicgames.gdx:gdx-bullet-platform:1.13.5:natives-arm64-v8a")
    implementation("com.badlogicgames.gdx:gdx-bullet-platform:1.13.5:natives-x86_64")
}
