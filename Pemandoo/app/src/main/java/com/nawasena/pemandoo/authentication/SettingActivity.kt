package com.nawasena.pemandoo.authentication

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.databinding.ActivitySettingBinding
import com.nawasena.pemandoo.others.AboutUsActivity
import com.nawasena.pemandoo.util.AppPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.btnBackProfile.setOnClickListener { finish() }
        binding.btnEditProfile.setOnClickListener { editProfile() }
        binding.btnLanguage.setOnClickListener { chooseLanguage() }
        binding.btnResetPassword.setOnClickListener { openResetPassword() }
        binding.btnAboutUs.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
        }
        binding.signOutButton.setOnClickListener { confirmLogout() }

        updateLanguageLabel()
        bindUser()
    }

    private fun bindUser() {
        val user = auth.currentUser
        if (user == null) {
            binding.textName.setText(R.string.guest_user)
            binding.textEmail.setText(R.string.not_signed_in)
            binding.textDateJoined.setText(R.string.guest_mode_description)
            binding.imageAvatar.setImageResource(R.drawable.ic_image)
            binding.btnEditProfile.setText(R.string.login_to_edit_profile)
            return
        }

        binding.textName.text = user.displayName ?: getString(R.string.guest_user)
        binding.textEmail.text = user.email ?: getString(R.string.not_signed_in)

        val created = user.metadata?.creationTimestamp
        if (created != null) {
            val formatter = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            binding.textDateJoined.text =
                getString(R.string.join_date_value, formatter.format(Date(created)))
        } else {
            binding.textDateJoined.text = getString(R.string.join_date_unknown)
        }

        if (user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .into(binding.imageAvatar)
        } else {
            binding.imageAvatar.setImageResource(R.drawable.ic_image)
        }
    }

    private fun editProfile() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, R.string.error_login_required, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        val input = EditText(this).apply {
            hint = getString(R.string.username)
            setText(user.displayName.orEmpty())
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, 8, padding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_profile)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, R.string.required_field, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val update = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user.updateProfile(update).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        firestore.collection("users").document(user.uid)
                            .set(mapOf("username" to newName), SetOptions.merge())
                        binding.textName.text = newName
                        Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.profile_update_failed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun chooseLanguage() {
        val options = arrayOf(
            getString(R.string.language_indonesian),
            getString(R.string.language_english)
        )
        val current = AppPreferences.getTtsLanguage(this)
        val selected = if (current == AppPreferences.LANGUAGE_ENGLISH) 1 else 0

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.tts_language)
            .setSingleChoiceItems(options, selected) { dialog, which ->
                val language = if (which == 1) {
                    AppPreferences.LANGUAGE_ENGLISH
                } else {
                    AppPreferences.LANGUAGE_INDONESIAN
                }
                AppPreferences.setTtsLanguage(this, language)
                updateLanguageLabel()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateLanguageLabel() {
        binding.tvLanguage.text = AppPreferences.languageDisplayName(
            AppPreferences.getTtsLanguage(this)
        )
    }

    private fun openResetPassword() {
        val destination = if (auth.currentUser == null) {
            ForgetPasswordActivity::class.java
        } else {
            ResetPasswordActivity::class.java
        }
        startActivity(Intent(this, destination))
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ -> signOut() }
            .show()
    }

    private fun signOut() {
        auth.signOut()
        try {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            GoogleSignIn.getClient(this, options).signOut()
        } catch (_: Exception) {
            // Google Sign-In mungkin belum pernah dipakai; tidak masalah.
        }

        val intent = Intent(this, OnBoardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
