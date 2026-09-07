plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

fun computeVersionName(): String {
    return System.getenv("CI_COMMIT_TAG") ?: "0.0.0"
}

fun computeVersionCode(): Int {
    val versionSplit = (System.getenv("CI_COMMIT_TAG") ?: "0.0.0").split(".")
    if (versionSplit.size != 3) {
        throw IllegalArgumentException("The version tag needs to be in the format major.minor.patch")
    }

    val major = versionSplit[0]
    val minor = versionSplit[1]
    val patch = versionSplit[2]

    val versionCode =
        buildString {
            append("10")
            append(
                ((major.toInt() * 100000) + (minor.toInt() * 10000) + patch.toInt()).toString(
                    10
                )
            )
        }.toInt()

    print("Versionname is ${major}.${minor}.${patch}")
    print("Versioncode is $versionCode")

    return versionCode
}

android {
    namespace = "dev.imanuel.abfuhr"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.imanuel.abfuhr"
        minSdk = 28
        targetSdk {
            version = release(37)
        }
        versionCode = computeVersionCode()
        versionName = computeVersionName()

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile =
                file(
                    System.getenv("ANDROID_KEY_STOREFILE")
                        ?: "/opt/secure/signing-key-jinya-plan.jks"
                )
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS") ?: "key0"
            keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isCrunchPngs = true
            isShrinkResources = true
            isProfileable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.kotlin.toString()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)

    implementation(platform(libs.io.insert.koin.bom))
    implementation(libs.io.insert.koin.core)
    implementation(libs.io.insert.koin.compose)
    implementation(libs.io.insert.koin.android)
    implementation(libs.io.insert.koin.androidx.workmanager)

    implementation(libs.accompanist.permissions)

    implementation(libs.androidx.adaptive.android)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    implementation(libs.compose.material.icons.extended)

    implementation(libs.play.services.location)
    implementation(libs.google.maps.compose)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(project(":sharedLogic"))
}