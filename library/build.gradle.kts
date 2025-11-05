import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

val androidMinSdkVersion = libs.versions.androidMinSdkVersion.get().toInt()
val androidTargetSdkVersion = libs.versions.androidTargetSdkVersion.get().toInt()
val verName = libs.versions.versionName.get()

android {
    namespace = "website.xihan.kv"
    compileSdk {
        version = release(androidTargetSdkVersion)
    }

    publishing {
        singleVariant("release") {

        }
    }

    defaultConfig {
        minSdk = androidMinSdkVersion

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
        freeCompilerArgs = listOf(
            "-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core.coroutines)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.xihan123"
                artifactId = "kv-storage"
                version = verName
            }
        }
    }
}