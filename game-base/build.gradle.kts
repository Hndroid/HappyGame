plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(deps.libs.androidx.appcompat.appcompat)
    implementation(deps.libs.material)
}
