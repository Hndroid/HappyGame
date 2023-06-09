package com.happiest.game.app.shared.dialog

import android.content.*
import android.view.*
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.happiest.game.R
import com.happiest.game.base.BaseDialog
import com.happiest.game.base.action.AnimAction


class CommonDialog {

    open class Builder<B : Builder<B>>(context: Context) : BaseDialog.Builder<B>(context) {

        private var autoDismiss = true

        private val containerLayout: ViewGroup? by lazy { findViewById(R.id.ll_ui_container) }
        private val titleView: TextView? by lazy { findViewById(R.id.tv_ui_title) }
        private val cancelView: TextView? by lazy { findViewById(R.id.tv_ui_cancel) }
        private val lineView: View? by lazy { findViewById(R.id.v_ui_line) }
        private val confirmView: TextView? by lazy { findViewById(R.id.tv_ui_confirm) }

        init {
            setContentView(R.layout.ui_dialog)
            setAnimStyle(AnimAction.ANIM_IOS)
            setGravity(Gravity.CENTER)
            setOnClickListener(cancelView, confirmView)
        }

        fun setCustomView(@LayoutRes id: Int): B {
            return setCustomView(LayoutInflater.from(getContext()).inflate(id, containerLayout, false))
        }

        fun setCustomView(view: View?): B {
            containerLayout?.addView(view, 1)
            return this as B
        }

        fun setTitle(@StringRes id: Int): B {
            return setTitle(getString(id))
        }

        fun setTitle(text: CharSequence?): B {
            titleView?.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            titleView?.text = text
            return this as B
        }

        fun setCancel(@StringRes id: Int): B {
            return setCancel(getString(id))
        }

        fun setCancel(text: CharSequence?): B {
            cancelView?.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            lineView?.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            cancelView?.text = text
            return this as B
        }

        fun setConfirm(@StringRes id: Int): B {
            return setConfirm(getString(id))
        }

        fun setConfirm(text: CharSequence?): B {
            confirmView?.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
            confirmView?.text = text
            return this as B
        }

        fun setAutoDismiss(dismiss: Boolean): B {
            autoDismiss = dismiss
            return this as B
        }

        fun autoDismiss() {
            if (autoDismiss) {
                dismiss()
            }
        }
    }
}
