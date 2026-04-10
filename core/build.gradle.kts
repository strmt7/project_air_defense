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

tasks.register<JavaExec>("runBattleMonteCarlo") {
    group = "verification"
    description = "Runs the shared headless battle simulation for balance analysis."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.airdefense.game.BattleSimulationCliKt")
    args(
        providers.gradleProperty("runs").orElse("300").get(),
        providers.gradleProperty("waves").orElse("1").get(),
        providers.gradleProperty("seconds").orElse("48").get(),
        providers.gradleProperty("step").orElse("0.05").get(),
        providers.gradleProperty("seed").orElse("20260411").get()
    )
}

tasks.test {
    useJUnitPlatform()
}
