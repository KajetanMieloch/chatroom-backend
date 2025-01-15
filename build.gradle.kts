plugins {
    kotlin("jvm") version "1.8.10" // Użyj najnowszej stabilnej wersji
    kotlin("plugin.serialization") version "1.8.10"
    application
}

application {
    mainClass.set("com.example.ApplicationKt") // Upewnij się, że ścieżka jest poprawna
}

repositories {
    mavenCentral()
}

dependencies {
dependencies {
    implementation("io.ktor:ktor-server-core:2.3.2")
    implementation("io.ktor:ktor-server-netty:2.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.2")
    implementation("io.ktor:ktor-server-call-logging:2.3.2")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    testImplementation(kotlin("test"))
}
}
