package com.happiest.game.app

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.os.StrictMode
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import com.happiest.game.BuildConfig
import com.happiest.game.app.shared.http.model.RequestServer
import com.happiest.game.app.shared.library.LibraryIndexScheduler
import com.happiest.game.app.shared.manager.ActivityStackManager
import com.happiest.game.app.shared.savesync.SaveSyncWork
import com.happiest.game.app.utils.android.DebugLoggerTree
import com.happiest.game.ext.feature.context.ContextHandler
import com.happiest.game.lib.injection.HasWorkerInjector
import com.happiest.game.app.shared.http.model.RequestHandler
import com.hjq.gson.factory.GsonFactory
import com.hjq.http.EasyConfig
import com.hjq.http.config.IRequestInterceptor
import com.hjq.http.model.HttpHeaders
import com.hjq.http.model.HttpParams
import com.hjq.http.request.HttpRequest
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.mmkv.MMKV
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.DaggerApplication
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject


class HappyGameApplication : DaggerApplication(), HasWorkerInjector {
    companion object {
        fun get(context: Context) = context.applicationContext as HappyGameApplication

        /**
         * 初始化第三方库
         */
        fun initSdk(application: Application) {

            // Bugly 异常捕捉
            CrashReport.initCrashReport(application)

            // Activity 栈管理初始化
            ActivityStackManager.getInstance().init(application)
            // Bugly 异常捕捉
            CrashReport.initCrashReport(application, BuildConfig.BUILD_TYPE, BuildConfig.DEBUG)

            // MMKV 初始化
            MMKV.initialize(application)

            // 网络请求框架初始化
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .build()

            EasyConfig.with(okHttpClient)
                // 是否打印日志
                .setLogEnabled(true)
                // 设置服务器配置
                .setServer(RequestServer())
                // 设置请求处理策略
                .setHandler(RequestHandler(application))
                // 设置请求重试次数
                .setRetryCount(1)
                .setInterceptor (object : IRequestInterceptor {
                    override fun interceptArguments(
                        httpRequest: HttpRequest<*>,
                        params: HttpParams,
                        headers: HttpHeaders
                    ) {

                    }
                })
                .setRetryCount(1)
                .setRetryTime(2000)
                .into()

            // 设置 Json 解析容错监听
            GsonFactory.setJsonCallback { typeToken: TypeToken<*>, fieldName: String?, jsonToken: JsonToken ->
                // 上报到 Bugly 错误列表
                CrashReport.postCatchedException(IllegalArgumentException("类型解析异常：$typeToken#$fieldName，后台返回的类型为：$jsonToken"))
            }


            // TODO 测试开启
            // 初始化日志打印
            if (BuildConfig.DEBUG) {

            }
            Timber.plant(DebugLoggerTree())
        }
    }

    @Inject lateinit var workerInjector: DispatchingAndroidInjector<ListenableWorker>

    @SuppressLint("CheckResult")
    override fun onCreate() {
        super.onCreate()
        HappyGame.initialize(this)
        initializeWorkManager()
        initSdk(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            enableStrictMode()
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }

    private fun initializeWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

        WorkManager.initialize(this, config)
        /* 判断是否主进程 */
        if (isMainProcess()) {
            SaveSyncWork.enqueueAutoWork(applicationContext, 0)
            LibraryIndexScheduler.scheduleCoreUpdate(applicationContext)
        }
    }

    private fun isMainProcess(): Boolean {
        return retrieveProcessName() == packageName
    }

    private fun retrieveProcessName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getProcessName()
        }

        val currentPID = Process.myPid()
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.runningAppProcesses
            .firstOrNull { it.pid == currentPID }
            ?.processName
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        ContextHandler.attachBaseContext(base)
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerHappyGameApplicationComponent.builder().create(this)
    }

    override fun workerInjector(): AndroidInjector<ListenableWorker> = workerInjector
}
