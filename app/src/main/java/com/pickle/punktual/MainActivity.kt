package com.pickle.punktual

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**
         * We assign the navController from the XML interface.
         * The Interface code contains everything needed
         * to link the fragment to the navGraph and to set it as default navHost
         */
        navController = findNavController(R.id.host_fragment)

        /**
         * We can call this function to make the navbar update accordingly to the navController
         * Android + Jetpack handle this for us
         */
        setupActionBarWithNavController(navController)
    }


    /**
     * This is called when the back button of the action bar is clicked
     */
    override fun onSupportNavigateUp(): Boolean {
        /**
         * We delegate the behavior of the "back" button from the action Bar
         * to the navigation controller
         */
        return navController.navigateUp()
    }

    override fun onBackPressed() {
        navController.navigateUp()
    }


}
