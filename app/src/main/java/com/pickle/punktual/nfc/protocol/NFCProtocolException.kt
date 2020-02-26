package com.pickle.nfcboilerplateapp.protocol

import android.content.Intent
import android.provider.Settings

open class NFCProtocolException(message: String, cause: Throwable? = null): Exception(message, cause)

class DisabledNfcException(message: String, cause: Throwable? = null): NFCProtocolException(message, cause)


fun nfcSettingsIntent(): Intent {
    return Intent(Settings.ACTION_NFC_SETTINGS)
}