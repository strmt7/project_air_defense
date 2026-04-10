plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx:1.14.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
