package com.pickle.punktual.team


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.ViewModelFactoryRepository

/**
 * A simple [Fragment] subclass.
 */
class LastUsersPositionFragment : Fragment() {

    val viewModel : TeamViewModel by viewModels {
        ViewModelFactoryRepository(PunktualApplication.repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_last_users_position, container, false)
    }


}
