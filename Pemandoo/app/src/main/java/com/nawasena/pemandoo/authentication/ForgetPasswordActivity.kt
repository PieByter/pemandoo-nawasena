package com.nawasena.pemandoo.authentication

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.databinding.ActivityForgetPasswordBinding

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val prefilledEmail = intent.getStringExtra(EXTRA_EMAIL)
        if (!prefilledEmail.isNullOrBlank()) {
            binding.edEmailForgot.setText(prefilledEmail)
        }

        binding.btnBackForgot.setOnClickListener { finish() }
        binding.btnSendReset.setOnClickListener { sendResetEmail() }
    }

    private fun sendResetEmail() {
        val email = binding.edEmailForgot.text?.toString()?.trim().orEmpty()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textInputEmailForgot.error = getString(R.string.invalid_email)
            return
        }
        binding.textInputEmailForgot.error = null
        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.reset_email_sent_title)
                        .setMessage(getString(R.string.reset_email_sent_message, email))
                        .setPositiveButton(android.R.string.ok) { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> getString(R.string.error_user_not_found)
                        is FirebaseNetworkException -> getString(R.string.error_network)
                        else -> task.exception?.localizedMessage
                            ?: getString(R.string.error_auth_generic)
                    }
                    if (isConfigurationError(task.exception)) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.auth_unavailable_title)
                            .setMessage(R.string.auth_unavailable_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSendReset.isEnabled = !loading
        binding.btnSendReset.text = if (loading) {
            getString(R.string.sending)
        } else {
            getString(R.string.send_reset_link)
        }
    }

    private fun isConfigurationError(exception: Exception?): Boolean {
        val message = exception?.message?.lowercase().orEmpty()
        return message.contains("blocked") ||
            message.contains("api key") ||
            message.contains("configuration") ||
            message.contains("internal error")
    }

    companion object {
        const val EXTRA_EMAIL = "email"
    }
}
