import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.lsplugin.resopt)
    alias(libs.plugins.lsplugin.lsparanoid)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

val androidMinSdkVersion = libs.versions.androidMinSdkVersion.get().toInt()
val androidTargetSdkVersion = libs.versions.androidTargetSdkVersion.get().toInt()
val verCode = libs.versions.versionCode.get().toInt()
val verName = libs.versions.versionName.get()


lsparanoid {
    seed = null
    classFilter = { true }
    includeDependencies = false
    variantFilter = { true }
}

android {
    namespace = "website.xihan.xposed"
    compileSdk {
        version = release(androidTargetSdkVersion)
    }

    signingConfigs {
        create("xihantest") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    defaultConfig {
        minSdk = androidMinSdkVersion
        versionCode = verCode
        versionName = verName

        signingConfig = signingConfigs.getByName("xihantest")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("xihantest")
        }
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
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packagingOptions.apply {
        resources.excludes += mutableSetOf(
            "META-INF/**", "**/*.properties", "schema/**", "**.bin"
        )
        dex.useLegacyPackaging = true
    }

    lint.checkReleaseBuilds = false

    dependenciesInfo.includeInApk = false
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
    implementation(libs.android.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.core.coroutines)
    compileOnly(libs.xposed.api)

    implementation(project(":library"))
}