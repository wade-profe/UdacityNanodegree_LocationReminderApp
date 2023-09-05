package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.utils.FirebaseUserLiveData

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

const val SIGN_IN_REQUEST_CODE = 1000
class AuthenticationActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityAuthenticationBinding = DataBindingUtil.setContentView(this, R.layout.activity_authentication)

        viewModel.authenticationState.observe(this, Observer {
            when(it){
                AuthenticationState.AUTHENTICATED -> {
                    intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else -> {
                    binding.login.setOnClickListener {
                        launchSignIn()
                    }
                }
            }
        })

    }

    private fun launchSignIn(){
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()

        getSignInResult.launch(intent)

    }

    private val getSignInResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == Activity.RESULT_OK){
                Toast.makeText(applicationContext, "Signed in Successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(applicationContext, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }
}

enum class AuthenticationState(){
    AUTHENTICATED, NOT_AUTHENTICATED
}
class AuthenticationViewModel: ViewModel() {

    val authenticationState: LiveData<AuthenticationState> = FirebaseUserLiveData().map {
        if(it != null){
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.NOT_AUTHENTICATED
        }
    }

}
