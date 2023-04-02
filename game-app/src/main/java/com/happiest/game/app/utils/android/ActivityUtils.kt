package com.happiest.game.app.utils.android

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.qmuiteam.qmui.widget.dialog.QMUIDialog

fun Activity.displayErrorDialog(messageId: Int, actionLabelId: Int, action: () -> Unit) {
    displayErrorDialog(resources.getString(messageId), resources.getString(actionLabelId), action)
}

fun Activity.displayErrorDialog(message: String, actionLabel: String, action: () -> Unit) {
    AlertDialog.Builder(this)
        .setMessage(message)
        .setPositiveButton(actionLabel) { _, _ -> action() }
        .setCancelable(false)
        .show()
}
