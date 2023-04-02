package com.happiest.game.app.shared.aop

import android.app.Activity
import android.util.Log
import com.happiest.game.app.shared.manager.ActivityStackManager
import com.happiest.game.app.utils.android.PermissionCallback
import com.hjq.permissions.XXPermissions
import com.tencent.bugly.crashreport.CrashReport
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import timber.log.Timber

@Aspect
class PermissionsAspect {
    /**
     * 方法切入点
     */
    @Pointcut("execution(@com.happiest.game.app.shared.aop.Permissions * *(..))")
    fun method() {}

    /**
     * 在连接点进行方法替换
     */
    @Around("method() && @annotation(permissions)")
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint, permissions: Permissions) {
        Log.d("TAG", "The activity has been destroyed and permission requests cannot be made")
        var activity: Activity? = null

        // 方法参数值集合
        val parameterValues: Array<Any?> = joinPoint.args
        for (arg: Any? in parameterValues) {
            if (arg !is Activity) {
                continue
            }
            activity = arg
            break
        }
        if ((activity == null) || activity.isFinishing || activity.isDestroyed) {
            activity = ActivityStackManager.getInstance().getTopActivity()
        }
        if ((activity == null) || activity.isFinishing || activity.isDestroyed) {
            Timber.e("The activity has been destroyed and permission requests cannot be made")
            return
        }
        requestPermissions(joinPoint, activity, permissions.value)
    }

    private fun requestPermissions(joinPoint: ProceedingJoinPoint, activity: Activity, permissions: Array<out String>) {
        XXPermissions.with(activity)
            .permission(*permissions)
            .request(object : PermissionCallback() {
                override fun onGranted(permissions: MutableList<String?>?, all: Boolean) {
                    if (all) {
                        try {
                            // 获得权限，执行原方法
                            joinPoint.proceed()
                        } catch (e: Throwable) {
                            CrashReport.postCatchedException(e)
                        }
                    }
                }
            })
    }
}
