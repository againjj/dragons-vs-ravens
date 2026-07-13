extra["pairedProjectDisplayName"] = "platform"

apply(from = "../gradle/paired-project.gradle.kts")

subprojects {
    group = "com.ayaziangames.platform"
}
