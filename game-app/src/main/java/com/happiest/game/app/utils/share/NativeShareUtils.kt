package com.happiest.game.app.utils.share

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File


class NativeShareUtils {

    companion object {
        private const val PACKAGE_WECHAT = "com.tencent.mm"
        private const val PACKAGE_QQ = "com.tencent.mobileqq"
        private const val PACKAGE_QZONG = "com.qzone"
        private const val PACKAGE_SINA = "com.sina.weibo"

        private const val ACTIVITY_SHARE_WECHAT_FRIEND = "com.tencent.mm.ui.tools.ShareImgUI" // 微信好友
        private const val ACTIVITY_SHARE_WECHAT_MOMENT = "com.tencent.mm.ui.tools.ShareToTimeLineUI" // 朋友圈
        private const val ACTIVITY_SHARE_QQ_FRIEND = "com.tencent.mobileqq.activity.JumpActivity" // QQ 分为图片和纯文本
        private const val ACTIVITY_SHARE_QQ_ZONE = "com.qzonex.module.operation.ui.QZonePublishMoodActivity" // QQ空间
        private const val ACTIVITY_SHARE_SINA_FRIEND = "com.sina.weibo.weiyou.share.WeiyouShareDispatcher" // 微博好友  setType("text/plain");
        private const val ACTIVITY_SHARE_SINA_CONTENT = "com.sina.weibo.composerinde.ComposerDispatchActivity" // 微博内容  setType("image/*");

        /**
         * 分享到QQ空间
         */
        fun shareImageToQQZone(image: File?, activity: Activity) {
            if (isInstalledSpecifiedApp(PACKAGE_QZONG, activity)) {
                val intent = Intent()
                val componentName = ComponentName(PACKAGE_QZONG, ACTIVITY_SHARE_QQ_ZONE)
                intent.component = componentName
                intent.action = "android.intent.action.SEND"
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_TEXT, "TEST")

                val authority = activity.packageName + ".provider"

                image?.let {
                    if (it.isFile && it.exists()) {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            FileProvider.getUriForFile(activity, authority, it)
                        } else {
                            Uri.fromFile(it)
                        }
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                    }
                }
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "您需要安装QQ空间客户端", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * 分享图片给QQ好友
         */
        fun shareImageToQQ(image: File?, activity: Activity) {
            if (isInstalledSpecifiedApp(PACKAGE_QQ, activity)) {
                val intent = Intent()
                val componentName = ComponentName(PACKAGE_QQ, ACTIVITY_SHARE_QQ_FRIEND)
                intent.component = componentName
                intent.action = Intent.ACTION_SEND
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_TEXT, "TEST")
                val authority = activity.packageName + ".provider"
                image?.let {
                    if (it.isFile && it.exists()) {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            FileProvider.getUriForFile(activity, authority, it)
                        } else {
                            Uri.fromFile(it)
                        }
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                    }
                }
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "您需要安装QQ客户端", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * 直接分享图片或文件给微信好友
         *
         * @param file      文件
         * @param isPicFile 是否为图片文件
         */
        fun shareToWechatFriend(file: File?, activity: Activity) {
            Timber.d("shareToWechatFriend: " + file!!.absolutePath)
            if (isInstalledSpecifiedApp(PACKAGE_WECHAT, activity)) {
                val intent = Intent()
                //分享精确到微信的页面，朋友圈页面，或者选择好友分享页面
                val comp = ComponentName(PACKAGE_WECHAT, ACTIVITY_SHARE_WECHAT_FRIEND)
                // 微信在6.7.3以后已经对多图分享进行了封杀了，请看网友分析：https://www.jianshu.com/p/1158d7c20a8b
                intent.component = comp
                intent.action = Intent.ACTION_SEND
                intent.type = "image/*"
                val authority = activity.packageName + ".provider"
                Timber.d("shareToWechatFriend: $authority")
                file?.let {
                    if (it.isFile && it.exists()) {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            FileProvider.getUriForFile(activity, authority, it)
                        } else {
                            Uri.fromFile(it)
                        }
                        Timber.d("shareToWechatFriend: " + uri.authority + " - " + uri.path)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                    }
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "您需要安装微信客户端", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * 直接分享文本和图片到微信朋友圈
         * 微信6.6.7以后已经不支持分享标题了，详情查看：https://www.jianshu.com/p/57935d90bf75
         * 微信6.7.3以后已经不支持多图分享了，详情查看：https://www.jianshu.com/p/1158d7c20a8b
         */
        fun shareImageToWeChatMomend(img: File?, activity: Activity) {
            if (isInstalledSpecifiedApp(PACKAGE_WECHAT, activity)) {
                val intent = Intent()
                //分享精确到微信的页面，朋友圈页面，或者选择好友分享页面
                val comp = ComponentName(PACKAGE_WECHAT, ACTIVITY_SHARE_WECHAT_MOMENT)
                // 微信在6.7.3以后已经对多图分享进行了封杀了，请看网友分析：https://www.jianshu.com/p/1158d7c20a8b
                intent.component = comp
                intent.action = Intent.ACTION_SEND
                intent.type = "image/*"
                val authority = activity.packageName + ".provider"
                img?.let {
                    if (it.isFile && it.exists()) {
                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            FileProvider.getUriForFile(activity, authority, img)
                        } else {
                            Uri.fromFile(img)
                        }
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                    }
                }
                // 微信6.6.7版本以后已经无法使用如下Kdescription来分享标题了，详情可查看网友分析：https://www.jianshu.com/p/57935d90bf75
                activity.startActivityForResult(intent, 101)
            } else {
                Toast.makeText(activity, "您需要安装微信客户端", Toast.LENGTH_LONG).show()
            }
        }

        /**
         * 是否存在指定包名的客户端
         * [pkg] com.tencent.mm / com.tencent.mobileqq / com.tencent.mobileqq
          */
        private fun isInstalledSpecifiedApp(pkg: String?, context: Context): Boolean {
            val pm = context.packageManager
            val pInfo = pm.getInstalledPackages(0)
            pInfo?.let {
                for (packageInfo in it) {
                    if (packageInfo.packageName.equals(pkg)) {
                        return true
                    }
                }
            }
            return false
        }

    }

}
