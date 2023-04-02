plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("android-aspectjx")
}

android {
    defaultConfig {
        versionCode = 1
        versionName = "1.0.0"
        applicationId = "com.happiest.game"
    }

    if (usePlayDynamicFeatures()) {
        println("Building Google Play version. Bundling dynamic features.")
        dynamicFeatures = mutableSetOf(
            ":lemuroid_core_desmume",
            ":lemuroid_core_dosbox_pure",
            ":lemuroid_core_fbneo",
            ":lemuroid_core_fceumm",
            ":lemuroid_core_gambatte",
            ":lemuroid_core_genesis_plus_gx",
            ":lemuroid_core_handy",
            ":lemuroid_core_mame2003_plus",
            ":lemuroid_core_mednafen_ngp",
            ":lemuroid_core_mednafen_pce_fast",
            ":lemuroid_core_mednafen_wswan",
            ":lemuroid_core_melonds",
            ":lemuroid_core_mgba",
            ":lemuroid_core_mupen64plus_next_gles3",
            ":lemuroid_core_pcsx_rearmed",
            ":lemuroid_core_ppsspp",
            ":lemuroid_core_prosystem",
            ":lemuroid_core_snes9x",
            ":lemuroid_core_stella"
        )
    }

    // Since some dependencies are closed source we make a completely free as in free speech variant.
//    flavorDimensions("opensource", "cores")

//    productFlavors {
//
//        create("free") {
//            dimension = "opensource"
//        }
//
//        create("play") {
//            dimension = "opensource"
//        }
//
//        // Include cores in the final apk
//        create("bundle") {
//            dimension = "cores"
//        }
//
//        // Download cores on demand (from GooglePlay or GitHub)
//        create("dynamic") {
//            dimension = "cores"
//        }
//    }

    // Stripping created some issues with some libretro cores such as ppsspp
    packagingOptions {
        doNotStrip("*/*/*_libretro_android.so")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/library_release.kotlin_module")
    }

    signingConfigs {
        maybeCreate("debug").apply {
            storeFile = file("$rootDir/debug.keystore")
            keyAlias = "key0"
            storePassword = "android"
            keyPassword = "android"
        }

        maybeCreate("release").apply {
            storeFile = file("$rootDir/debug.keystore")
            keyAlias = "key0"
            storePassword = "android"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("release") {
            isDebuggable = true
            isJniDebuggable = true
            isZipAlignEnabled = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs["release"]
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "口袋掌机")
            resValue("color", "main_color", "#00c64e")
            resValue("color", "main_color_light", "#9de3aa")
            buildConfigField("String", "BUGLY_ID", "\"-\"")
            // 清单占位符
            addManifestPlaceholders(mapOf("UM_KEY" to deps.configs.UMENG_APP_KEY,
                "QQ_ID" to deps.configs.QQ_APP_ID, "QQ_SECRET" to deps.configs.QQ_APP_SECRET,
                "WX_ID" to deps.configs.WX_APP_ID, "WX_SECRET" to deps.configs.WX_APP_SECRET))
        }
        getByName("debug") {
            isDebuggable = true
            isJniDebuggable = true
            isZipAlignEnabled = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs["debug"]
            versionNameSuffix = "-DEBUG"
            resValue("string", "app_name", "口袋掌机-测试版本")
            resValue("color", "main_color", "#2196F3")
            resValue("color", "main_color_light", "#FF000000")
            buildConfigField("String", "BUGLY_ID", "\"-\"")
            // 清单占位符
            addManifestPlaceholders(mapOf("UM_KEY" to deps.configs.UMENG_APP_KEY,
                "QQ_ID" to deps.configs.QQ_APP_ID, "QQ_SECRET" to deps.configs.QQ_APP_SECRET,
                "WX_ID" to deps.configs.WX_APP_ID, "WX_SECRET" to deps.configs.WX_APP_SECRET))
        }
    }

    // 代码警告配置
    lintOptions {
        disable += setOf("MissingTranslation", "ExtraTranslation")
        isCheckReleaseBuilds = false
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // AOP 配置（exclude 和 include 二选一）
    // 需要进行配置，否则就会引发冲突，具体表现为：
    // 第一种：编译不过去，报错：java.util.zip.ZipException：Cause: zip file is empty
    // 第二种：编译能过去，但运行时报错：ClassNotFoundException: Didn't find class on path: DexPathList
    aspectjx{

        // 排除一些第三方库的包名（Gson、 LeakCanary 和 AOP 有冲突）
        // exclude 'androidx', 'com.google', 'com.squareup', 'org.apache', 'com.alipay', 'com.taobao', 'versions.9'
        // 只对以下包名做 AOP 处理
        include(android.defaultConfig.applicationId)
    }

    // 添加 so 库的 jniLibs
    sourceSets["main"].jniLibs{
        srcDirs("libs")
    }
}

dependencies {
    implementation(project(":game-util"))
    implementation(project(":game-app-shared"))
    implementation(project(":game-metadata-db"))
    implementation(project(":game-touchinput"))
    implementation(project(":game-widget"))
    implementation(project(":game-core"))
    implementation(project(":game-libs"))
    implementation(project(":game-app-ext-free"))
//    "playImplementation"(project(":game-app-ext-play"))
    implementation(deps.libs.androidx.navigation.navigationFragment)
    implementation(deps.libs.androidx.navigation.navigationUi)
    implementation(deps.libs.material)
    implementation(deps.libs.coil)
    implementation(deps.libs.androidx.appcompat.constraintLayout)
    implementation(deps.libs.androidx.appcompat.appcompat)
    implementation(deps.libs.androidx.preferences.preferencesKtx)
    implementation(deps.libs.rxbindings.core)
    implementation(deps.libs.rxbindings.appcompat)
    implementation(deps.libs.arch.work.runtime)
    implementation(deps.libs.arch.work.runtimeKtx)
    implementation(deps.libs.arch.work.rxjava2)
    implementation(deps.libs.androidx.lifecycle.commonJava8)
    implementation(deps.libs.androidx.lifecycle.reactiveStreams)
    implementation(deps.libs.epoxy.expoxy)
    implementation(deps.libs.epoxy.paging)

    kapt(deps.libs.epoxy.processor)
    kapt(deps.libs.androidx.lifecycle.processor)

    implementation(deps.libs.androidx.leanback.leanback)
    implementation(deps.libs.androidx.leanback.leanbackPreference)
    implementation(deps.libs.androidx.leanback.leanbackPaging)

    implementation(deps.libs.androidx.appcompat.recyclerView)
    implementation(deps.libs.androidx.paging.common)
    implementation(deps.libs.androidx.paging.runtime)
    implementation(deps.libs.androidx.paging.rxjava2)
    implementation(deps.libs.androidx.room.common)
    implementation(deps.libs.androidx.room.runtime)
    implementation(deps.libs.androidx.room.rxjava2)
    implementation(deps.libs.autodispose.android.archComponents)
    implementation(deps.libs.autodispose.android.core)
    implementation(deps.libs.autodispose.core)
    implementation(deps.libs.dagger.android.core)
    implementation(deps.libs.dagger.android.support)
    implementation(deps.libs.dagger.core)
    implementation(deps.libs.koptional)
    implementation(deps.libs.koptionalRxJava2)
    implementation(deps.libs.kotlinxCoroutinesAndroid)
    implementation(deps.libs.okHttp3)
    implementation(deps.libs.okio)
    implementation(deps.libs.retrofit)
    implementation(deps.libs.retrofitRxJava2)
    implementation(deps.libs.rxAndroid2)
    implementation(deps.libs.rxJava2)
    implementation(deps.libs.rxPermissions2)
    implementation(deps.libs.rxPreferences)
    implementation(deps.libs.rxRelay2)
    implementation(deps.libs.rxKotlin2)
    implementation(deps.libs.guava)
    implementation(deps.libs.androidx.documentfile)
    implementation(deps.libs.androidx.leanback.tvProvider)
    implementation(deps.libs.harmony)

//    implementation(deps.libs.libretrodroid)
    implementation(deps.libs.txqmui)
    implementation(deps.libs.arch.qmui.qmuiArch)
    implementation(deps.libs.arch.qmui.qmuiArchCompiler)
    implementation(deps.libs.xxPermission)
    implementation(deps.libs.aspectjrt)
    implementation(deps.libs.easyHttp)
    implementation(deps.libs.crashreport)
    implementation(deps.libs.nativecrashreport)
//    implementation(deps.libs.androidUtils)
    implementation(deps.libs.mmkv)
    implementation(deps.libs.gson)
    implementation(deps.libs.gsonFactory)

//    implementation(deps.share.common)
//    implementation(deps.share.asms)
//    implementation(deps.share.push)
//    implementation(deps.share.shareCore)
//    implementation(deps.share.shareBoard)
//    implementation(deps.share.shareWx)
//    implementation(deps.share.androidWithoutMta)
//    implementation(deps.share.shareQq)
//    implementation(deps.share.qqopensdk)

    // Uncomment this when using a local aar file.
    //implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    kapt(deps.libs.dagger.android.processor)
    kapt(deps.libs.dagger.compiler)

}

fun usePlayDynamicFeatures(): Boolean {
    val task = gradle.startParameter.taskRequests.toString()
    return task.contains("Play") && task.contains("Dynamic")
}
