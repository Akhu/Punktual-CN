package com.pickle.nfcboilerplateapp

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class NFCData(val data: String): Parcelable