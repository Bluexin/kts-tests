plugins {
    kotlin("jvm")
}

group = "be.bluexin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-common")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation(project(":kts-scriptdef"))
}

tasks.test {
    useJUnitPlatform()
}