import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun signingValue(propertyKey: String, envKey: String): String? {
    val fromFile = keystoreProperties.getProperty(propertyKey)
    return fromFile ?: System.getenv(envKey)
}

android {
    namespace = "com.offlineupi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.offlineupi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("storeFile", "RELEASE_STORE_FILE")
            val storePwd = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
            val keyAliasValue = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
            val keyPwd = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")

            if (storeFilePath != null && storePwd != null && keyAliasValue != null && keyPwd != null) {
                storeFile = if (storeFilePath.startsWith("/")) {
                    file(storeFilePath)
                } else {
                    rootProject.file(storeFilePath)
                }
                storePassword = storePwd
                keyAlias = keyAliasValue
                keyPassword = keyPwd
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.zxing.embedded)
    implementation(libs.androidx.viewpager2)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}