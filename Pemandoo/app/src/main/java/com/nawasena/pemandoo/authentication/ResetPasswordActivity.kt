package com.nawasena.pemandoo.authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.databinding.ActivityResetPasswordBinding

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        binding.btnBackReset.setOnClickListener { finish() }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding.edEmailReset.setText(user.email.orEmpty())
        binding.edEmailReset.isEnabled = false
        binding.btnResetPassword.setOnClickListener { updatePassword() }
    }

    private fun updatePassword() {
        val newPassword = binding.edPassword.text?.toString().orEmpty()
        val confirmation = binding.edPasswordConfirm.text?.toString().orEmpty()

        if (newPassword.length < 6) {
            binding.tiPasswordReset.error = getString(R.string.password_too_short)
            return
        }
        binding.tiPasswordReset.error = null

        if (newPassword != confirmation) {
            binding.tiConfirmPasswordReset.error = getString(R.string.passwords_do_not_match)
            return
        }
        binding.tiConfirmPasswordReset.error = null

        val user = auth.currentUser ?: return
        setLoading(true)
        user.updatePassword(newPassword)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, R.string.password_updated, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthWeakPasswordException ->
                            getString(R.string.password_too_short)
                        is FirebaseAuthRecentLoginRequiredException ->
                            getString(R.string.error_recent_login)
                        is FirebaseNetworkException -> getString(R.string.error_network)
                        else -> task.exception?.localizedMessage
                            ?: getString(R.string.error_auth_generic)
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnResetPassword.isEnabled = !loading
        binding.btnResetPassword.text = if (loading) {
            getString(R.string.saving)
        } else {
            getString(R.string.reset_password)
        }
    }
}
