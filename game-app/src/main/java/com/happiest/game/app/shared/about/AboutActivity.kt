package com.happiest.game.app.shared.about

import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.happiest.game.R
import com.happiest.game.app.shared.about.bean.UpdateConfigData
import com.happiest.game.app.shared.aop.CheckNet
import com.happiest.game.app.shared.aop.Permissions
import com.happiest.game.app.shared.aop.SingleClick
import com.happiest.game.base.BaseActivity
import com.happiest.game.lib.library.SystemID
import com.hjq.gson.factory.GsonFactory
import com.hjq.http.EasyHttp
import com.hjq.http.listener.OnDownloadListener
import com.hjq.http.model.HttpMethod
import com.hjq.permissions.Permission
import java.io.File


class AboutActivity: BaseActivity() {

    var toolbar: Toolbar? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_about
    }

    override fun initView() {
        setupToolbar()
        setOnClickListener({
            displayLemuroidHelp()
        }, R.id.cl_about_help)

        setOnClickListener( {
            checkUpdateConfig()
        }, R.id.cl_about_update)

        setOnClickListener({
            displayOpenSourceProtocolDialog()
        } , R.id.cl_about_opsp)

        setOnClickListener({
            showPopularizeDialog()
        }, R.id.cl_about_mark)
    }

    override fun initData() {
    }

    @SingleClick
    fun showPopularizeDialog() {
        PopularizeDialog.Builder(this@AboutActivity).show()
    }

    /**
     * 开源协议页面
     */
    @SingleClick
    private fun displayOpenSourceProtocolDialog() {
        val message = getString(" ")
        AlertDialog.Builder(this)
            .setTitle(R.string.copyrightInfo)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { _, _ ->
            }
            .show()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.title = "关于"
        actionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * 玩法帮助
     */
    private fun displayLemuroidHelp() {
        val systemFolders = SystemID.values()
            .map { it.dbname }
            .map { "<i>$it</i>" }
            .joinToString(", ")

        val message = getString(R.string.lemuroid_help_content).replace("\$SYSTEMS", systemFolders)
        AlertDialog.Builder(this)
            .setMessage(Html.fromHtml(message))
            .setPositiveButton(R.string.ok) { _, _ ->
            }
            .setNeutralButton(R.string.openBrowser) {_,_ -> }
            .show()
    }

    @CheckNet
    @SingleClick
    @Permissions(Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE, Permission.REQUEST_INSTALL_PACKAGES)
    private fun checkUpdateConfig() {
        val configFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "config.txt")

        EasyHttp.download(this@AboutActivity)
            .method(HttpMethod.GET)
            .file(configFile)
            .url("http://baidu.cn/pocketgames/config.json?v=${System.currentTimeMillis()}")
            .listener(object: OnDownloadListener {
                override fun onStart(file: File?) {

                }

                override fun onProgress(file: File?, progress: Int) {

                }

                override fun onComplete(file: File?) {

                }

                override fun onError(file: File?, e: Exception?) {

                }

                override fun onEnd(file: File?) {
                    file?.let {
                        if (it.exists()) {
                            val configData = GsonFactory.getSingletonGson().fromJson(it.readText(), UpdateConfigData::class.java)
                            showUpdataDialog(configData)
                        }
                    }
                }
            })
            .start()

    }

    private fun showUpdataDialog(data: UpdateConfigData?) {
        data?.let {
            // 升级对话框
            UpdateDialog.Builder(AboutActivity@this)
                // 版本名
                .setVersionName(it.version)
                // 是否强制更新
                .setForceUpdate(it.isforce)
                // 更新日志
                .setUpdateLog(it.update_log)
                // 下载 URL
                .setDownloadUrl(it.path)
                // 文件 MD5
                .setFileMd5(it.md5)
                .show()
        }
    }

    companion object {
        fun launchAboutActivity(activity: Activity) {
            val intent = Intent(activity, AboutActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
