plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace="com.nirmaldoor.billing"
    compileSdk=35
    defaultConfig {
        applicationId="com.nirmaldoor.billing"
        minSdk=24
        targetSdk=35
        versionCode=2
        versionName="2.0"
    }
    buildTypes { release { isMinifyEnabled=false } }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget="17" }
}
