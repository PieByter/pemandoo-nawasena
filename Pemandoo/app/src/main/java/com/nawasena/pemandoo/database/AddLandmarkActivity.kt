package com.nawasena.pemandoo.database

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.databinding.ActivityAddLandmarkBinding
import com.nawasena.pemandoo.util.ImageStorage
import com.nawasena.pemandoo.util.LandmarkPhotoLoader
import com.nawasena.pemandoo.viewmodel.LandmarkViewModel
import java.io.File
import java.util.UUID

class AddLandmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddLandmarkBinding
    private lateinit var viewModel: LandmarkViewModel

    private var editingLandmark: Landmark? = null
    private var selectedPhoto: String? = null
    private var cameraFile: File? = null
    private var isSaving = false
    private var fieldsPopulated = false

    private val galleryLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val copiedPath = ImageStorage.copyToAppStorage(this, uri)
        if (copiedPath == null) {
            Toast.makeText(this, R.string.image_load_failed, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        selectedPhoto = copiedPath
        LandmarkPhotoLoader.load(binding.ivShowImage, copiedPath)
    }

    private val cameraLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val file = cameraFile
        if (result.resultCode == RESULT_OK && file != null && file.exists() && file.length() > 0) {
            selectedPhoto = file.absolutePath
            LandmarkPhotoLoader.load(binding.ivShowImage, file.absolutePath)
        } else {
            file?.delete()
        }
        cameraFile = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddLandmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LandmarkViewModel::class.java]

        binding.btnBackAdd.setOnClickListener { finish() }
        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
        binding.btnCamera.setOnClickListener { openCamera() }
        binding.buttonSave.setOnClickListener { saveLandmark() }
        binding.buttonDelete.setOnClickListener { confirmDelete() }

        val landmarkId = intent.getStringExtra(EXTRA_LANDMARK_ID)
        if (!landmarkId.isNullOrBlank()) {
            binding.tvAddTitle.setText(R.string.edit_landmark)
            binding.buttonDelete.visibility = android.view.View.VISIBLE
            viewModel.observeLandmark(landmarkId).observe(this) { landmark ->
                if (landmark == null || fieldsPopulated) return@observe
                fieldsPopulated = true
                editingLandmark = landmark
                populateFields(landmark)
            }
        } else {
            binding.tvAddTitle.setText(R.string.add_landmark)
        }
    }

    private fun populateFields(landmark: Landmark) {
        binding.editTextName.setText(landmark.name)
        binding.editTextLatitude.setText(landmark.latitude.toString())
        binding.editTextLongitude.setText(landmark.longitude.toString())
        binding.editTextDescription.setText(landmark.overview)
        binding.editTextArchitecturalFeatures.setText(landmark.architecturalFeatures)
        binding.editTextInterestingFacts.setText(landmark.interestingFacts)
        binding.editTextHistoricalSignificance.setText(landmark.historicalSignificance)
        binding.editTextHours.setText(landmark.hours)
        binding.editTextEntryFee.setText(landmark.entryFee)
        binding.editTextGeofenceRadius.setText(landmark.radiusMeters.toString())
        selectedPhoto = landmark.photo
        LandmarkPhotoLoader.load(binding.ivShowImage, landmark.photo)
    }

    private fun openCamera() {
        val fileAndUri = ImageStorage.createCameraFile(this)
        if (fileAndUri == null) {
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        cameraFile = fileAndUri.first
        try {
            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(android.provider.MediaStore.EXTRA_OUTPUT, fileAndUri.second)
                addFlags(
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            cameraLauncher.launch(intent)
        } catch (exception: Exception) {
            fileAndUri.first.delete()
            cameraFile = null
            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveLandmark() {
        if (isSaving) return

        val name = binding.editTextName.text.toString().trim()
        val latitude = binding.editTextLatitude.text.toString().trim().toDoubleOrNull()
        val longitude = binding.editTextLongitude.text.toString().trim().toDoubleOrNull()
        val radius = binding.editTextGeofenceRadius.text.toString().trim()
            .toFloatOrNull() ?: Landmark.DEFAULT_RADIUS_METERS

        if (name.isEmpty()) {
            binding.editTextName.error = getString(R.string.required_field)
            binding.editTextName.requestFocus()
            return
        }
        if (latitude == null || latitude !in -90.0..90.0) {
            binding.editTextLatitude.error = getString(R.string.invalid_latitude)
            binding.editTextLatitude.requestFocus()
            return
        }
        if (longitude == null || longitude !in -180.0..180.0) {
            binding.editTextLongitude.error = getString(R.string.invalid_longitude)
            binding.editTextLongitude.requestFocus()
            return
        }
        if (radius <= 0f) {
            binding.editTextGeofenceRadius.error = getString(R.string.invalid_radius)
            binding.editTextGeofenceRadius.requestFocus()
            return
        }

        val existing = editingLandmark
        val landmark = Landmark(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude,
            photo = selectedPhoto.orEmpty(),
            overview = binding.editTextDescription.text.toString().trim(),
            architecturalFeatures = binding.editTextArchitecturalFeatures.text.toString().trim(),
            interestingFacts = binding.editTextInterestingFacts.text.toString().trim(),
            historicalSignificance = binding.editTextHistoricalSignificance.text.toString().trim(),
            hours = binding.editTextHours.text.toString().trim(),
            entryFee = binding.editTextEntryFee.text.toString().trim(),
            radiusMeters = radius.coerceIn(50f, 2000f),
            isUserCreated = existing?.isUserCreated ?: true,
            updatedAt = System.currentTimeMillis()
        )

        isSaving = true
        binding.buttonSave.isEnabled = false
        binding.buttonSave.text = getString(R.string.saving)

        viewModel.saveLandmark(landmark) { success, error ->
            isSaving = false
            binding.buttonSave.isEnabled = true
            binding.buttonSave.setText(R.string.save_landmark)

            if (success) {
                Toast.makeText(this, R.string.landmark_saved, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    error ?: getString(R.string.landmark_save_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmDelete() {
        val landmark = editingLandmark ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_landmark)
            .setMessage(getString(R.string.delete_landmark_confirmation, landmark.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteLandmark(landmark.id) { success ->
                    if (success) {
                        if (landmark.photo.startsWith("/")) {
                            ImageStorage.delete(landmark.photo)
                        }
                        Toast.makeText(this, R.string.landmark_deleted, Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, R.string.landmark_delete_failed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .show()
    }

    companion object {
        const val EXTRA_LANDMARK_ID = "landmark_id"
    }
}
