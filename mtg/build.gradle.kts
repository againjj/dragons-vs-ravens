extra["pairedProjectDisplayName"] = "Magic: the Gathering"

apply(from = "../gradle/paired-project.gradle.kts")

subprojects {
    group = "com.ayaziangames.mtg"
}
