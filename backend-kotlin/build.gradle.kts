plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("io.ktor.plugin") version "2.3.12"
    application
}

group = "com.bloodsync"
version = "1.0.0"

application {
    mainClass.set("com.bloodsync.ApplicationKt")
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-server-cors:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("io.ktor:ktor-server-call-logging:2.3.12")
    implementation("io.ktor:ktor-server-auth:2.3.12")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Ktor Client (for webhook outbound calls)
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")

    // Firebase Admin SDK (Java/Kotlin compatible)
    implementation("com.google.firebase:firebase-admin:9.3.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.1")  // For Firebase ListenableFuture

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // JavaMail (for SMTP / OTP email delivery)
    implementation("com.sun.mail:jakarta.mail:2.0.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // UUID
    implementation("com.fasterxml.uuid:java-uuid-generator:5.0.0")

    // DotEnv
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Testing
    testImplementation("io.ktor:ktor-server-tests:2.3.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.0")
}

kotlin {
    jvmToolchain(17)
}
