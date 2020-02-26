package com.pickle.nfcboilerplateapp.protocol

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import timber.log.Timber
import java.nio.charset.Charset

class NFCProtocolIntent(
    private val activity: Activity,
    override val listener: NFCProtocol.NFCReaderListener?): NFCProtocol {

    private var pendingIntent: PendingIntent? = null
    private enum class State {
        IDLE,
        INIT,
        LOCKED,
        UNLOCKED
    }

    private var state = State.IDLE
    private var nfcAdapter: NfcAdapter? = null

    private fun init(): Boolean {
        if(state != State.IDLE) {
            Timber.w("Invalid state for init. State=%s", state)
            return false
        }
        if(nfcAdapter == null || !nfcAdapter!!.isEnabled){
            NfcAdapter.getDefaultAdapter(activity)?.let {
                if(!it.isEnabled){
                    listener?.onNFCError(DisabledNfcException("Cannot initialize NFC adapter"))
                    return false
                }
                nfcAdapter = it
            }
        }

        pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0)
        switchToState(State.INIT)
        return true
    }

    override fun lockNFC() {
        if(state == State.IDLE){
            if(!init()){
                return
            }
        }

        val intentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, intentFilters, null)
        switchToState(State.LOCKED)
    }

    override fun unlockNFC() {
        Timber.i("Unlocking NFC")
        if(state != State.LOCKED){
            Timber.w("Invalid state for unlock. State=%s", state)
            return
        }

        nfcAdapter?.disableForegroundDispatch(activity)
        switchToState(State.UNLOCKED)
    }

    override suspend fun readNFCTag(intent: Intent) {

        Timber.i("Reading NFC")

        when(val data = nfcTask(intent)) {
            is NFCTaskResultSuccess -> listener?.onNFCDataReady(data.message)
            is NFCTaskResultFailure -> listener?.onNFCError(data.exception)
        }

    }

    override fun writeNFCTag(intent: Intent, data: String) {
        when(val result = nfcTask(intent, messageToWrite = data)) {
            is NFCTaskResultSuccess -> {
                listener?.onNFCDataReady(result.message)
                Timber.i("Writing NFC with Data $data")
            }
            is NFCTaskResultFailure -> {
                Timber.e("Error writing NFC data $data")
                listener?.onNFCError(result.exception)

            }
        }

    }

    private fun switchToState(state: State) {
        Timber.d("Switching from ${this.state} to $state")
        this.state = state
    }

    private fun nfcTask(intent: Intent, messageToWrite: String = "") : NFCTaskResult {
        if(state != State.LOCKED){
            Timber.w("Cannot notify listener, invalid state = ${this@NFCProtocolIntent.state}")
        }
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val ndef = Ndef.get(tag)
        try {
            ndef.connect()

            if (!ndef.isConnected) {
                return taskFailureFromMessage("Cannot connect to NDEF")
            }
            return if (messageToWrite.isEmpty()) {
                readNdefMessage(ndef)
            } else {
                writeNdefMessage(ndef, messageToWrite)
            }
        } catch (exception: Exception){
            Timber.e(exception)
            return taskFailureFromMessage("Unhandled Exception=${exception.message}")
        }
    }


}


private sealed class NFCTaskResult
private data class NFCTaskResultSuccess(val message: String): NFCTaskResult()
private data class NFCTaskResultFailure(val exception: Exception): NFCTaskResult()

private fun taskFailureFromMessage(message: String): NFCTaskResult =
    NFCTaskResultFailure(NFCProtocolException(message))

private fun readNdefMessage(ndef: Ndef) : NFCTaskResult {
    val ndefMessage = ndef.ndefMessage ?: return taskFailureFromMessage("Cannot read TAG: NDEF message is null")

    val record = ndefMessage.records.first()
    val message = parseNdefRecord(record)
    ndef.close()

    Timber.d("Message read from NFC TAG")
    return NFCTaskResultSuccess(message)
}

private fun writeNdefMessage(ndef: Ndef, messageToWrite: String): NFCTaskResult {
    if(!ndef.isWritable){
        return taskFailureFromMessage("Cannot write TAG: Read-only TAG")
    }

    ndef.writeNdefMessage(dataToNdefTextMessage(messageToWrite))
    ndef.close()
    Timber.i("Data written into NFC Tag, message = $messageToWrite")
    return NFCTaskResultSuccess(messageToWrite)
}

fun dataToNdefTextMessage(messageToWrite: String): NdefMessage {
    val record = NdefRecord.createMime("text/plan", messageToWrite.toByteArray(NFC_CHARSET))
    return NdefMessage(arrayOf(record))
}

fun parseNdefRecord(record: NdefRecord): String {
    return when (record.type) {
        NdefRecord.RTD_URI -> record.toUri().toString()
        else -> record.payload.toString(NFC_CHARSET)
    }
}

private val NFC_CHARSET = Charset.forName("US-ASCII")
