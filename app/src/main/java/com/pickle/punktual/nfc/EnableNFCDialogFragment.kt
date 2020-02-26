package com.pickle.nfcboilerplateapp

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class EnableNFCDialogFragment: DialogFragment() {

    var onPositiveClick : (() -> Unit)? = null
    var onNegativeClick : (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return with(AlertDialog.Builder(context)) {
            setTitle("NFC Disabled")
            setMessage("Please enable NFC in the settings to scan an NFC Tag.")
            setPositiveButton("Go to NFC Settings") { _,_ -> onPositiveClick?.invoke() }
            setNegativeButton("Cancel") { _,_ -> onNegativeClick?.invoke() }
            create()
        }

    }

}