package com.happiest.game.app.shared.about

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.happiest.game.R
import com.happiest.game.app.shared.dialog.adapter.AppAdapter
import com.happiest.game.app.shared.umeng.Platform
import com.happiest.game.base.BaseAdapter

import com.happiest.game.base.BaseDialog
import com.happiest.game.base.action.AnimAction

class PopularizeDialog {

    class Builder constructor(context: Context) : BaseDialog.Builder<Builder>(context),
        BaseAdapter.OnItemClickListener {

        private val recyclerView: RecyclerView? by lazy { findViewById(R.id.rv_share_list) }
        private val cancle: Button? by lazy { findViewById(R.id.wechat_account_cancle) }
        private var adapter: SaveAdapter? = null
        private var listener: OnListener? = null


        init {
            setContentView(R.layout.activity_popularize)
            setAnimStyle(AnimAction.ANIM_BOTTOM)
            setCanceledOnTouchOutside(true)
            val data: MutableList<SaveBean> = ArrayList()
            data.add(
                SaveBean(
                    getDrawable(R.drawable.share_wechat_ic)!!,
                    getString(R.string.share_platform_wechat)!!,
                    Platform.WECHAT
                )
            )
            data.add(
                SaveBean(
                    getDrawable(R.drawable.share_pic_ic)!!,
                    getString(R.string.share_save_pic)!!,
                    Platform.LOCAL
                )
            )
            adapter = SaveAdapter(context)
            adapter!!.setData(data)
            adapter!!.setOnItemClickListener(this)
            recyclerView?.layoutManager = GridLayoutManager(context, 5)
            recyclerView?.adapter = adapter
            setOnClickListener(cancle)
        }

        private class SaveAdapter(context: Context) : AppAdapter<SaveBean>(context) {
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

        fun setListener(listener: OnListener?): Builder = apply {
            this.listener = listener
        }

        class SaveBean(
            /** 分享图标  */
            val shareIcon: Drawable,
            /** 分享名称  */
            val shareName: String,
            /** 分享平台  */
            val sharePlatform: Platform
        )

        override fun onItemClick(recyclerView: RecyclerView?, itemView: View?, position: Int) {

        }

        override fun onClick(view: View) {
            if (view == cancle) {
                dismiss()
            }
            listener?.onCancel(getDialog())
        }

        interface OnListener {

            /**
             * 输入完成时回调
             *
             * @param password      输入的密码
             */
            fun onCompleted(dialog: BaseDialog?, password: String)

            /**
             * 点击取消时回调
             */
            fun onCancel(dialog: BaseDialog?) {}
        }

    }
}
