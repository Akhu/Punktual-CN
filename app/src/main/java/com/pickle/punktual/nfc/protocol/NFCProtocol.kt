package com.pickle.nfcboilerplateapp.protocol

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

interface NFCProtocol : LifecycleObserver{

    val listener: NFCReaderListener?

    interface NFCReaderListener {
        fun onNFCError(error: Throwable)
        fun onNFCDataReady(data: String)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun lockNFC()

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun unlockNFC()

    suspend fun readNFCTag(intent: Intent)
    fun writeNFCTag(intent: Intent, data: String)

}