plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {

    defaultConfig {

        consumerProguardFiles(file("proguard-rules.pro"))

        externalNativeBuild {
            cmake {
                cppFlags.add("-std=c++17")
            }
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.10.2"
        }
    }

//    // 添加 so 库的 jniLibs
//    sourceSets["main"].jniLibs{
//        srcDirs("libs")
//    }
}

dependencies {
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation(deps.libs.kotlin.stdlibJdk7)
    implementation(deps.libs.rxRelay2)
    implementation(deps.libs.rxKotlin2)
    implementation(deps.libs.rxJava2)
    implementation(deps.libs.androidx.lifecycle.runtimeKtx)


    implementation(deps.libs.androidx.ktx.core)
    implementation(deps.libs.androidx.appcompat.appcompat)
    implementation(deps.libs.material)
}
