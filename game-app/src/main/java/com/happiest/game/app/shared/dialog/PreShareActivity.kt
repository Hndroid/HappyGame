package com.happiest.game.app.shared.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.happiest.game.R
import com.happiest.game.app.HappyGame
import com.happiest.game.app.shared.ImmersiveActivity
import com.happiest.game.app.shared.dialog.adapter.AppAdapter
import com.happiest.game.app.shared.umeng.Platform
import com.happiest.game.app.utils.image.ImageUtils
import com.happiest.game.app.utils.share.NativeShareUtils
import com.happiest.game.base.BaseAdapter
import com.happiest.game.lib.util.subscribeBy
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.ByteArrayInputStream
import javax.inject.Inject

class PreShareActivity: ImmersiveActivity(), BaseAdapter.OnItemClickListener {

    @Inject lateinit var sharedPreferences: Lazy<SharedPreferences>

    private val recyclerView: RecyclerView? by lazy { findViewById(R.id.rv_share_list) }
    private var adapter: ShareAdapter?= null
    private val shareCard: CardView? by lazy { findViewById(R.id.share_card_cv) }
    private val cancleBtn: Button? by lazy { findViewById(R.id.btn_cancel_share) }
    private val rxSharedPreferences = Single.fromCallable {
        RxSharedPreferences.create(sharedPreferences.get())
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_share_inflate)

        rxSharedPreferences
            .map {
                val bitmapStr = it.getString(INTEN_KEY_SHARE_IMAGE, "").get()
                val inputStream = ByteArrayInputStream(Base64.decode(bitmapStr, 0))
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribeBy {
                val shareView = findViewById<ImageView>(R.id.capture_share_screen)
                shareView.scaleType = ImageView.ScaleType.FIT_XY
                shareView.setImageBitmap(it)
            }

        val data: MutableList<ShareBean> = ArrayList()
        data.add(ShareBean(getDrawable(R.drawable.share_wechat_ic)!!, getString(R.string.share_platform_wechat)!!, Platform.WECHAT))
        data.add(ShareBean(getDrawable(R.drawable.share_moment_ic)!!, getString(R.string.share_platform_moment)!!, Platform.CIRCLE))
        data.add(ShareBean(getDrawable(R.drawable.share_qq_ic)!!, getString(R.string.share_platform_qq)!!, Platform.QQ))
        data.add(ShareBean(getDrawable(R.drawable.share_qzone_ic)!!, getString(R.string.share_platform_qzone)!!, Platform.QZONE))
        data.add(ShareBean(getDrawable(R.drawable.share_pic_ic)!!, getString(R.string.share_save_pic)!!, Platform.LOCAL))
        adapter =  ShareAdapter(this@PreShareActivity)
        adapter!!.setData(data)
        adapter!!.setOnItemClickListener(this)
        recyclerView?.layoutManager = GridLayoutManager(this@PreShareActivity, data.size)
        recyclerView?.adapter = adapter
        cancleBtn?.setOnClickListener { finish() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_push_none, R.anim.activity_push_bottom_out)
    }

    companion object {
        const val INTEN_KEY_SHARE_IMAGE = "share_image"

        fun start(activity: Activity) {
            val intent = Intent(activity, PreShareActivity::class.java)
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.activity_push_bottom_in, 0)
        }
    }

    private class ShareAdapter(context: Context) : AppAdapter<ShareBean>(context) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder()
        }

        private inner class ViewHolder : AppViewHolder(R.layout.share_item) {
            private val imageView: ImageView? by lazy { findViewById(R.id.iv_share_image) }
            private val textView: TextView? by lazy { findViewById(R.id.tv_share_text) }
            override fun onBindView(position: Int) {
                getItem(position).apply {
                    imageView?.setImageDrawable(shareIcon)
                    textView?.text = shareName
                }
            }
        }
    }

    class ShareBean (
        /** 分享图标  */
        val shareIcon: Drawable,
        /** 分享名称  */
        val shareName: String,
        /** 分享平台  */
        val sharePlatform: Platform
    )

    override fun onItemClick(recyclerView: RecyclerView?, itemView: View?, position: Int) {
        val platform = adapter!!.getItem(position).sharePlatform
        shareCard?.let {
            val image = ImageUtils.genFileFromView(this@PreShareActivity, it)
            when(platform) {
                Platform.WECHAT -> {
                    NativeShareUtils.shareToWechatFriend(image, this@PreShareActivity)
                }
                Platform.CIRCLE -> {
                    NativeShareUtils.shareImageToWeChatMomend(image, this@PreShareActivity)
                }
                Platform.QQ -> {
                    NativeShareUtils.shareImageToQQ(image, this@PreShareActivity)
                }
                Platform.QZONE -> {
                    NativeShareUtils.shareImageToQQZone(image, this@PreShareActivity)
                }
                Platform.LOCAL -> {
                    if (image.exists()) {
                        MediaStore.Images.Media.insertImage(contentResolver, BitmapFactory.decodeFile(image.absolutePath), image.name, null)
                        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        val uri = Uri.fromFile(image)
                        intent.data = uri
                        sendBroadcast(intent)
                        Toast.makeText(this@PreShareActivity, "保存图片成功", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        HappyGame.getHandler().postDelayed({
            finish()
        }, 50)

    }
}
