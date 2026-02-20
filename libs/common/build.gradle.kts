plugins {
    id("loglens.kotlin-jvm")
}

group = "me.manulorenzo"
version = "unspecified"

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
