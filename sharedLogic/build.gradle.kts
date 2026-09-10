import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.serialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
            linkerOpts("-lsqlite3")
        }
    }

    android {
        namespace = "dev.imanuel.abfuhr.sharedLogic"
        compileSdk {
            version = release(37)
        }
        minSdk = 28

        compilerOptions {
            jvmTarget = JvmTarget.JVM_23
        }
        androidResources {
            enable = true
        }
    }

    sourceSets {
        sourceSets.commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.io.insert.koin.core)
            implementation(libs.xmlutil.core)
            implementation(libs.xmlutil.serialization)
            implementation(libs.kotlinx.html)
        }

        sourceSets.androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqlite.android)
        }

        sourceSets.iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
    }
}

sqldelight {
    databases {
        register("AbfallDatabase") {
            packageName.set("dev.imanuel.abfuhr.database")
        }
    }
}
