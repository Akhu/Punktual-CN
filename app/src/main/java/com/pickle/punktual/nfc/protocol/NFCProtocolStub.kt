package com.pickle.nfcboilerplateapp.protocol

import android.content.Intent
import timber.log.Timber

class NFCProtocolStub(override val listener: NFCProtocol.NFCReaderListener?) : NFCProtocol {

    companion object{
        private var counter = 1
    }

    override fun lockNFC() {
        Timber.i("Locking NFC")
    }

    override fun unlockNFC() {
        Timber.i("Unlocking NFC")
    }

    override suspend fun readNFCTag(intent: Intent) {
        Timber.i("Reading NFC")
        listener?.onNFCDataReady("http://bondgadget/nfc/$counter")
        counter++
    }

    override fun writeNFCTag(intent: Intent, data: String) {
        Timber.i("Writing NFC with Data $data")
    }
}