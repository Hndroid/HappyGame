package com.happiest.game.app.shared.aop

import android.app.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.happiest.game.R
import com.happiest.game.app.shared.manager.ActivityStackManager
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut

@Aspect
class CheckNetAspect {

    /**
     * 方法切入点
     */
    @Pointcut("execution(@com.happiest.game.app.shared.aop.CheckNet * *(..))")
    fun method() {}

    /**
     * 在连接点进行方法替换
     */
    @Around("method() && @annotation(checkNet)")
    @Throws(Throwable::class)
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint, checkNet: CheckNet) {
        val application: Application = ActivityStackManager.getInstance().getApplication()
        val manager: ConnectivityManager? =
            ContextCompat.getSystemService(application, ConnectivityManager::class.java)
        if (manager != null) {
            val info: NetworkInfo? = manager.activeNetworkInfo
            // 判断网络是否连接
            if (info == null || !info.isConnected) {
                Toast.makeText(application.baseContext, R.string.common_network_hint, Toast.LENGTH_LONG).show()
                return
            }
        }
        //执行原方法
        joinPoint.proceed()
    }
}
