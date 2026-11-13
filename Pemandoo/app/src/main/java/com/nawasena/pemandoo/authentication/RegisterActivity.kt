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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.database.MapsActivity
import com.nawasena.pemandoo.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

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
            if (exception.statusCode == 10) {
                showAuthUnavailable()
            } else {
                Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        binding.btnBackRegister.setOnClickListener {
            finish()
        }

        binding.tvLoginNow.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.tvGuestRegister.setOnClickListener {
            navigateToMap()
        }

        binding.btnFacebook.setOnClickListener {
            Toast.makeText(this, R.string.feature_not_available, Toast.LENGTH_SHORT).show()
        }

        binding.btnApple.setOnClickListener {
            Toast.makeText(this, R.string.feature_not_available, Toast.LENGTH_SHORT).show()
        }

        binding.btnRegister.setOnClickListener {
            val username = binding.edUsernameRegister.text?.toString()?.trim().orEmpty()
            val email = binding.edEmailRegister.text?.toString()?.trim().orEmpty()
            val password = binding.edPasswordRegister.text?.toString().orEmpty()
            val confirmation = binding.edConfirmPasswordRegister.text?.toString().orEmpty()

            if (username.length < 3) {
                binding.textInputUsernameRegister.error = getString(R.string.username_too_short)
                return@setOnClickListener
            }
            binding.textInputUsernameRegister.error = null

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.textInputEmailRegister.error = getString(R.string.invalid_email)
                return@setOnClickListener
            }
            binding.textInputEmailRegister.error = null

            if (password.length < 6) {
                binding.textInputPasswordRegister.error = getString(R.string.password_too_short)
                return@setOnClickListener
            }
            binding.textInputPasswordRegister.error = null

            if (password != confirmation) {
                binding.textInputConfirmPasswordRegister.error =
                    getString(R.string.passwords_do_not_match)
                return@setOnClickListener
            }
            binding.textInputConfirmPasswordRegister.error = null

            registerUser(username, email, password)
        }

        binding.btnGoogle.setOnClickListener {
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun registerUser(username: String, email: String, password: String) {
        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user == null) {
                        setLoading(false)
                        Toast.makeText(this, R.string.error_auth_generic, Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }

                    val profileUpdate = UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()

                    user.updateProfile(profileUpdate).addOnCompleteListener {
                        saveUserProfile(user, username)
                        Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT)
                            .show()
                        navigateToMap()
                    }
                } else {
                    setLoading(false)
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
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
                    val user = auth.currentUser
                    if (user == null) {
                        setLoading(false)
                        Toast.makeText(this, R.string.error_auth_generic, Toast.LENGTH_LONG).show()
                        return@addOnCompleteListener
                    }
                    val username = user.displayName
                        ?: user.email?.substringBefore('@')
                        ?: getString(R.string.guest_user)
                    saveUserProfile(user, username)
                    Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show()
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

    private fun saveUserProfile(user: FirebaseUser, username: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to user.email.orEmpty(),
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection("users").document(user.uid)
            .set(userData, SetOptions.merge())
            .addOnFailureListener { exception ->
                // Profil di Firestore bersifat pelengkap. Pendaftaran Firebase
                // Authentication tetap dianggap berhasil.
                Log.w(TAG, "Gagal menyimpan profil pengguna", exception)
            }
    }

    private fun navigateToMap() {
        if (hasNavigated) return
        hasNavigated = true
        startActivity(Intent(this, MapsActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnGoogle.isEnabled = !loading
        binding.tvLoginNow.isEnabled = !loading
        binding.btnRegister.text = if (loading) {
            getString(R.string.loading)
        } else {
            getString(R.string.register)
        }
    }

    private fun authErrorMessage(exception: Exception?): String = when (exception) {
        is FirebaseAuthWeakPasswordException -> getString(R.string.password_too_short)
        is FirebaseAuthUserCollisionException -> getString(R.string.error_email_in_use)
        is FirebaseAuthInvalidCredentialsException -> getString(R.string.invalid_email)
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
        private const val TAG = "RegisterActivity"
    }
}
