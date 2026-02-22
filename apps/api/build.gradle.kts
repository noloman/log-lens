plugins {
    id("loglens.kotlin-jvm")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jpa)
}

group = "me.manulorenzo"
version = "0.0.1-SNAPSHOT"

dependencies {
    implementation(project(":libs:common"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation("com.tngtech.archunit:archunit-junit4:1.4.1")
}

noArg {
    annotation("jakarta.persistence.Entity")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
