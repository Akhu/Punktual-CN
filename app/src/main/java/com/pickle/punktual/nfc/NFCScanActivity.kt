package com.pickle.punktual.nfc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.view.animation.AnimationUtils
import com.pickle.nfcboilerplateapp.EnableNFCDialogFragment
import com.pickle.nfcboilerplateapp.NFCData
import com.pickle.nfcboilerplateapp.protocol.DisabledNfcException
import com.pickle.nfcboilerplateapp.protocol.NFCProtocol
import com.pickle.nfcboilerplateapp.protocol.NFCProtocolIntent
import com.pickle.nfcboilerplateapp.protocol.NFCProtocolStub
import com.pickle.nfcboilerplateapp.protocol.nfcSettingsIntent
import com.pickle.punktual.BuildConfig
import com.pickle.punktual.R
import kotlinx.android.synthetic.main.activity_nfc_scan.*
import kotlinx.coroutines.*
import timber.log.Timber

private const val VIBRATION_DURATION = 300

/**
 * To read a NFC tag, we need to lock the NFC sensor first.
 * Then you need to release the sensor when you are done
 */
class NFCScanActivity : AppCompatActivity(), NFCProtocol.NFCReaderListener {

    companion object {
        const val NFC_MODE_READ = 0
        const val NFC_MODE_WRITE = 1
        const val EXTRA_NFC_MODE = "extraNfcMode"
        const val EXTRA_NFC_DATA = "extraNfcData"
    }

    private var nfcMode = NFC_MODE_READ

    private lateinit var nfcProtocol: NFCProtocol

    private lateinit var vibrator: Vibrator

    private var dataToWrite: String = ""

    private val nfcCoroutines = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nfc_scan)

        val mode = intent.getIntExtra(EXTRA_NFC_MODE, NFC_MODE_READ)
        dataToWrite = intent.getStringExtra(EXTRA_NFC_DATA)?: ""

        switchNfcMode(mode)

        imageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.blink))

        nfcProtocol = if ( BuildConfig.NFC_SIMULATOR_ENABLED) {
            NFCProtocolStub(this)
        } else {
            NFCProtocolIntent(this, this)
        }

        lifecycle.addObserver(nfcProtocol)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if(BuildConfig.NFC_SIMULATOR_ENABLED){
            nfcCoroutines.launch {
                nfcProtocol.readNFCTag(Intent())
            }
        }

    }

    private fun switchNfcMode(mode: Int) {
        this.nfcMode = mode
        when(mode) {
            NFC_MODE_READ -> {
                instructionTextView.text = "Scan the NFC Tag to READ data"
            }
            NFC_MODE_WRITE -> {
                instructionTextView.text = "Scan the NFC Tag to Write data"
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if(!isNFCTagIntent(intent)) {
            return
        }

        when(nfcMode) {
            NFC_MODE_READ -> nfcCoroutines.launch { nfcProtocol.readNFCTag(intent) }
            NFC_MODE_WRITE -> {
                nfcCoroutines.launch {
                    nfcProtocol.writeNFCTag(intent, dataToWrite)
                }
            }
        }
    }



    override fun onNFCError(error: Throwable) {
        Timber.i("Error $error")
        when(error) {
            is DisabledNfcException -> showEnableNFCDialog()
            else -> Timber.e(error, "Something went wrong with the NFC")
        }
    }

    private fun showEnableNFCDialog() {
        val dialog = EnableNFCDialogFragment()
        dialog.onNegativeClick = {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        dialog.onPositiveClick = {
            startNFCSettingsActivity()
        }

        dialog.show(supportFragmentManager, "EnableNFCDialog")
    }

    private fun startNFCSettingsActivity() {
        startActivity(nfcSettingsIntent())
    }

    override fun onNFCDataReady(data: String) {
        vibrator.vibrate(VIBRATION_DURATION.toLong())
        Timber.i("Data from NFC received $data")

        val intent = Intent()
        intent.putExtra("data", NFCData(data = data))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun isNFCTagIntent(intent: Intent): Boolean {
        Timber.i("Intent received = ${intent.action}")
        return when(intent.action){
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED ->  true
            else ->  false
        }
    }
}
