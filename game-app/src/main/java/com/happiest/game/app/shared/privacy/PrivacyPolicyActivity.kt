package com.happiest.game.app.shared.privacy

import android.app.Activity
import android.content.Intent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.happiest.game.R
import com.happiest.game.base.BaseActivity

class PrivacyPolicyActivity: BaseActivity() {

    private var toolbar: Toolbar? = null
    private var actionBar: ActionBar? = null
    private val mWebView: WebView? by lazy { findViewById(R.id.privacy_wv) }

    override fun getLayoutId(): Int {
        return R.layout.activity_privacy_policy
    }

    override fun initView() {
        setupToolbar()
        setupWebView()
    }

    override fun initData() {
        when(intent.getStringExtra(START_TYPPE)) {
            USER_AGREEMENT -> {
                actionBar?.title = "用户协议"
                mWebView?.loadUrl(USER_AGREEMENT_URL)
            }
            PRIVACY_POLICY -> {
                actionBar?.title = "隐私政策"
                mWebView?.loadUrl(PRIVACY_POLICY_URL)
            }
            DISCLAIMER -> {
                actionBar?.title = "免责申明"
                mWebView?.loadUrl(DISCLAIMER_URL)
            }
            else -> {}
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupWebView() {
        mWebView?.settings?.javaScriptEnabled = true
        mWebView?.webViewClient = WebViewClient()
        val settings = mWebView?.settings
        settings?.setSupportZoom(true)
        settings?.textZoom = 80
    }

    companion object {
        const val USER_AGREEMENT = "User_Agreement"
        const val PRIVACY_POLICY = "Privacy_Policy"
        const val DISCLAIMER = "Disclaimer"
        private const val START_TYPPE = "intent_typpe"

        private const val BASE_URL = "file:///android_asset/"
        private const val USER_AGREEMENT_URL = BASE_URL + "happygame_privacy-policy.html"
        private const val PRIVACY_POLICY_URL = BASE_URL + "happygame_user-terms.html"
        private const val DISCLAIMER_URL = BASE_URL + "happygame_disclaimer.html"

        fun start(activity: Activity, type: String) {
            val intent = Intent(activity, PrivacyPolicyActivity::class.java)
            intent.putExtra(START_TYPPE, type)
            activity.startActivity(intent)
        }
    }
}
