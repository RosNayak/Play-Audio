package com.roshan.playaudio.ui

import android.app.AlertDialog
import android.app.Dialog
import android.app.Notification
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.roshan.playaudio.listeners.ReadPermissionDialogButtonClickedListener

class ReadStorageRequestDialog(
    private val listener : ReadPermissionDialogButtonClickedListener
) : DialogFragment() {

    private val PERMISSION_DIALOG_TITLE : String = "Access required"
    private val PERMISSION_DIALOG_MESSAGE : String = "Please allow access to storage to get audio files."
    private val POSITIVE_BUTTON_TEXT : String = "OK"
    private val NEGATIVE_BUTTON_TEXT : String = "CANCEL"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val requestPermissionAlertDialog = AlertDialog.Builder(context)
        requestPermissionAlertDialog.setTitle(PERMISSION_DIALOG_TITLE)
            .setMessage(PERMISSION_DIALOG_MESSAGE)
            .setPositiveButton(POSITIVE_BUTTON_TEXT, DialogInterface.OnClickListener { _, _ ->
                listener.positiveButtonClicked()
                dismiss()
            })
            .setNegativeButton(NEGATIVE_BUTTON_TEXT, DialogInterface.OnClickListener { _, _ ->
                dismiss()
                listener.negativeButtonClicked()
            })
        return requestPermissionAlertDialog.create()
    }
}