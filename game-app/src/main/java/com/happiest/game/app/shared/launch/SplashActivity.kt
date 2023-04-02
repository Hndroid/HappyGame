package com.happiest.game.app.shared.launch

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

import com.happiest.game.R
import com.happiest.game.app.HappyGame
import com.happiest.game.app.mobile.feature.main.MainActivity
import com.happiest.game.app.shared.ImmersiveActivity
import com.happiest.game.app.shared.manager.ActivityStackManager
import com.happiest.game.app.shared.privacy.PrivacyPolicyActivity
import dagger.Lazy
import javax.inject.Inject

class SplashActivity: ImmersiveActivity() {

    @Inject
    lateinit var sharedPreferences: Lazy<SharedPreferences>

    /**
     * 记录进入SplashActivity的时间。
     */
    var enterTime: Long = 0

    /**
     * 判断是否正在跳转或已经跳转到下一个界面。
     */
    var isForwarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(null)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enterTime = System.currentTimeMillis()
        delayToForward()
    }

    /**
     * 设置闪屏界面的最大延迟跳转，让用户不至于在闪屏界面等待太久。
     */
    private fun delayToForward() {
        Thread(Runnable {
            HappyGame.sleep(MAX_WAIT_TIME.toLong())
            forwardToNextActivity()
        }).start()
    }

    /**
     * 跳转到下一个Activity。如果在闪屏界面停留的时间还不足规定最短停留时间，则会在这里等待一会，保证闪屏界面不至于一闪而过。
     */
    @Synchronized
    open fun forwardToNextActivity() {
        if (!isForwarding) { // 如果正在跳转或已经跳转到下一个界面，则不再重复执行跳转
            isForwarding = true
            val currentTime = System.currentTimeMillis()
            val timeSpent = currentTime - enterTime
            if (timeSpent < MIN_WAIT_TIME) {
                HappyGame.sleep(MIN_WAIT_TIME - timeSpent)
            }
            runOnUiThread {
                val isFer = sharedPreferences.get().getBoolean(KEY_AGREE_PRIVACY, false)
                if (isFer) {
                    if (ActivityStackManager.getInstance().isForeground()) {
                        MainActivity.start(this@SplashActivity)
                    } else {
                        ActivityStackManager.getInstance().finishAllActivities()
                    }
                } else {
                    showPrivacyDialog()
                }
            }
        }
    }

    override fun onBackPressed() {
        // 屏蔽手机的返回键
    }

    private fun showPrivacyDialog() {
        val privacyDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_privacy_info)
            .setCancelable(false)
            .setPositiveButton(R.string.agree_and_continue) { _, _ ->
                sharedPreferences.get().edit().apply {
                    this.putBoolean(KEY_AGREE_PRIVACY, true)
                }.commit()
                HappyGame.getHandler().postDelayed({
                    MainActivity.start(this@SplashActivity)
                }, 150)
            }
            .setNegativeButton(R.string.exist_app) { _, _ ->
                ActivityStackManager.getInstance().finishAllActivities()
            }
            .show()

        privacyDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(getColor(R.color.black25))
        // 用户服务协议
        privacyDialog.findViewById<TextView>(R.id.privacy_policy_tv1)?.setOnClickListener {
            PrivacyPolicyActivity.start(this@SplashActivity, PrivacyPolicyActivity.USER_AGREEMENT)
        }
        // 隐私政策
        privacyDialog.findViewById<TextView>(R.id.privacy_policy_tv2)?.setOnClickListener {
            PrivacyPolicyActivity.start(this@SplashActivity, PrivacyPolicyActivity.PRIVACY_POLICY)
        }
        // 免责申明
        privacyDialog.findViewById<TextView>(R.id.privacy_policy_tv3)?.setOnClickListener {
            PrivacyPolicyActivity.start(this@SplashActivity, PrivacyPolicyActivity.DISCLAIMER)
        }
    }

    companion object {
        const val KEY_AGREE_PRIVACY = "agree_privacy"
        /**
         * 应用程序在闪屏界面最短的停留时间。
         */
        const val MIN_WAIT_TIME = 2000

        /**
         * 应用程序在闪屏界面最长的停留时间。
         */
        const val MAX_WAIT_TIME = 5000
    }


}
