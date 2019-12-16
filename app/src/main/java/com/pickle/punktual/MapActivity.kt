package com.pickle.punktual

import android.annotation.SuppressLint
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.pickle.punktual.user.User

import kotlinx.android.synthetic.main.activity_map.*

class MapActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        PunktualApplication.repo.getCurrentUser().observe(this, Observer { updateUi(it) })

        //Get Location
        //Map fragment
        //Display POIs
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(user: User) {
        pseudoTextView.text = "${user.username} is connected with id: ${user.id}"
    }

}
