package com.pickle.punktual

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class RegisterDialogFragment : DialogFragment() {
    var onPositiveClick: ((userName: String) -> Unit)? = null
    var onNegativeClick: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_TEXT

        return with(AlertDialog.Builder(context)) {
            setView(editText)
            setTitle("Create a new Profile")
            setMessage("Enter a pseudo that other people might understand")
            setPositiveButton("Register") { _, _ -> onPositiveClick?.invoke(editText.text.toString()) }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
                onNegativeClick?.invoke() }
            create()
        }
    }
}