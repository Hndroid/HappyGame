package com.happiest.game.app.shared.dialog

import android.content.*
import android.view.*
import android.widget.TextView
import androidx.annotation.StringRes
import com.happiest.game.R
import com.happiest.game.app.shared.aop.SingleClick
import com.happiest.game.base.BaseDialog

class MyHelpTipDialog {

    class Builder constructor(context: Context) : CommonDialog.Builder<Builder>(context) {

        private var listener: OnListener? = null
        private val messageView: TextView?

        init {
            setCustomView(R.layout.dialog_show_message)
            messageView = findViewById(R.id.tv_message_message)
        }

        fun setMessage(@StringRes id: Int): Builder = apply {
            setMessage(getString(id))
        }

        fun setMessage(text: CharSequence?): Builder = apply {
            messageView?.text = text
        }

        fun setListener(listener: OnListener?): Builder = apply {
            this.listener = listener
        }

        override fun create(): BaseDialog {
            // 如果内容为空就抛出异常
            if (("" == messageView?.text.toString())) {
                throw IllegalArgumentException("Dialog message not null")
            }
            return super.create()
        }

        @SingleClick
        override fun onClick(view: View) {
            when (view.id) {
                R.id.tv_ui_confirm -> {
                    autoDismiss()
                    listener?.onConfirm(getDialog())
                }
                R.id.tv_ui_cancel -> {
                    autoDismiss()
                    listener?.onCancel(getDialog())
                }
            }
        }
    }

    interface OnListener {

        /**
         * 点击确定时回调
         */
        fun onConfirm(dialog: BaseDialog?)

        /**
         * 点击取消时回调
         */
        fun onCancel(dialog: BaseDialog?) {}
    }
}
