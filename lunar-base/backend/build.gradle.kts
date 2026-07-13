import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":platform:backend"))

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

val standardCardsScript = layout.projectDirectory.file("src/main/resources/card-sets/standard-cards.kts")
val generatedStandardDeckDirectory = layout.buildDirectory.dir("generated/sources/standardDeck/kotlin")
val generateStandardDeckSource by tasks.registering {
    val generatedSource = generatedStandardDeckDirectory.map {
        it.file("com/ayaziangames/lunarbase/cards/GeneratedStandardDeck.kt")
    }
    inputs.file(standardCardsScript)
    outputs.file(generatedSource)
    doLast {
        val output = generatedSource.get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """package com.ayaziangames.lunarbase.cards

internal val generatedStandardDeckDefinition: LunarBaseDeckDefinition =
${standardCardsScript.asFile.readText()}
"""
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generatedStandardDeckDirectory)
}

tasks.named("compileKotlin") {
    dependsOn(generateStandardDeckSource)
}

val java21Launcher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    javaLauncher.set(java21Launcher)
}
