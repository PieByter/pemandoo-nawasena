package com.nawasena.pemandoo.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.database.MapsActivity
import com.nawasena.pemandoo.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    private var hasNavigated = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            setLoading(false)
            return@registerForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                setLoading(false)
                Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show()
            } else {
                firebaseAuthWithGoogle(idToken)
            }
        } catch (exception: ApiException) {
            Log.w(TAG, "Google sign in failed", exception)
            setLoading(false)
            if (isGoogleConfigurationError(exception)) {
                showAuthUnavailable()
            } else {
                Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        binding.btnBackLogin.setOnClickListener {
            finish()
        }

        binding.tvForgetPassword.setOnClickListener {
            startActivity(Intent(this, ForgetPasswordActivity::class.java))
        }

        binding.tvRegisterNow.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.tvGuestLogin.setOnClickListener {
            navigateToMap()
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(this, R.string.feature_not_available, Toast.LENGTH_SHORT).show()
        }

        binding.btnApple.setOnClickListener {
            Toast.makeText(this, R.string.feature_not_available, Toast.LENGTH_SHORT).show()
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.edEmailLogin.text?.toString()?.trim().orEmpty()
            val password = binding.edPasswordLogin.text?.toString().orEmpty()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textInputEmailLogin.error = getString(R.string.invalid_email)
                return@setOnClickListener
            }
            binding.textInputEmailLogin.error = null
            if (password.length < 6) {
                binding.textInputPasswordLogin.error = getString(R.string.password_too_short)
                return@setOnClickListener
            }
            binding.textInputPasswordLogin.error = null
            signInWithEmailPassword(email, password)
        }

        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    navigateToMap()
                } else {
                    setLoading(false)
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    if (isConfigurationError(task.exception)) {
                        showAuthUnavailable()
                    } else {
                        Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    navigateToMap()
                } else {
                    setLoading(false)
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (isConfigurationError(task.exception)) {
                        showAuthUnavailable()
                    } else {
                        Toast.makeText(this, authErrorMessage(task.exception), Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
    }

    private fun navigateToMap() {
        if (hasNavigated) return
        hasNavigated = true
        startActivity(Intent(this, MapsActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.tvRegisterNow.isEnabled = !loading
        binding.btnLogin.text = if (loading) {
            getString(R.string.loading)
        } else {
            getString(R.string.login)
        }
    }

    private fun authErrorMessage(exception: Exception?): String = when (exception) {
        is FirebaseAuthInvalidCredentialsException -> getString(R.string.error_invalid_credentials)
        is FirebaseAuthInvalidUserException -> getString(R.string.error_user_not_found)
        is FirebaseAuthUserCollisionException -> getString(R.string.error_email_in_use)
        is FirebaseNetworkException -> getString(R.string.error_network)
        else -> exception?.localizedMessage ?: getString(R.string.error_auth_generic)
    }

    private fun isConfigurationError(exception: Exception?): Boolean {
        val message = exception?.message?.lowercase().orEmpty()
        return message.contains("blocked") ||
            message.contains("api key") ||
            message.contains("configuration") ||
            message.contains("internal error")
    }

    private fun isGoogleConfigurationError(exception: ApiException): Boolean {
        return exception.statusCode == 10 // DEVELOPER_ERROR
    }

    private fun showAuthUnavailable() {
        if (isFinishing || isDestroyed) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.auth_unavailable_title)
            .setMessage(R.string.auth_unavailable_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_as_guest) { _, _ -> navigateToMap() }
            .show()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            navigateToMap()
        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
