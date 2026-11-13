package com.nawasena.pemandoo.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.data.LandmarkRepository
import com.nawasena.pemandoo.data.PemandooDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Activity-scoped ViewModel shared by MapsActivity and all map fragments.
 * Keeping the repository and location client here avoids creating a new
 * ViewModel with a custom factory in every screen (one of the original crashes).
 */
class LandmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LandmarkRepository(
        PemandooDatabase.get(application).landmarkDao(),
        application
    )

    val landmarks: LiveData<List<Landmark>> = repository.landmarks.asLiveData()

    private val _currentLocation = MutableLiveData<Location?>()
    val currentLocation: LiveData<Location?> = _currentLocation

    private val _isSeeded = MutableLiveData(false)
    val isSeeded: LiveData<Boolean> = _isSeeded

    private val locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private var locationCallback: LocationCallback? = null
    private var seedJob: Job? = null

    init {
        seedJob = viewModelScope.launch {
            try {
                repository.seedIfNeeded()
            } catch (exception: Exception) {
                // Biarkan layar menampilkan pesan kosong daripada menggantung.
            } finally {
                _isSeeded.value = true
            }
        }
    }

    fun observeLandmark(id: String): LiveData<Landmark?> = repository.observeLandmark(id).asLiveData()

    fun saveLandmark(landmark: Landmark, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.save(landmark)
                onComplete(true, null)
            } catch (exception: Exception) {
                onComplete(false, exception.localizedMessage)
            }
        }
    }

    fun deleteLandmark(id: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                onComplete(true)
            } catch (exception: Exception) {
                onComplete(false)
            }
        }
    }

    fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Starts periodic location updates. Calling this method more than once is
     * safe; the second call is ignored while updates are already active.
     */
    fun startLocationUpdates() {
        if (locationCallback != null || !hasLocationPermission()) return

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { _currentLocation.value = it }
            }
        }
        locationCallback = callback

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            locationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && _currentLocation.value == null) {
                        _currentLocation.value = location
                    }
                }
                .addOnFailureListener {
                    // The periodic updates may still succeed.
                }
        } catch (exception: SecurityException) {
            locationCallback = null
        }
    }

    fun stopLocationUpdates() {
        val callback = locationCallback ?: return
        try {
            locationClient.removeLocationUpdates(callback)
        } catch (_: Exception) {
            // Updates may already have been removed by the system.
        }
        locationCallback = null
    }

    fun nearestLandmark(latitude: Double, longitude: Double): Landmark? {
        return landmarks.value.orEmpty().minByOrNull {
            it.distanceTo(latitude, longitude)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        seedJob?.cancel()
    }

    companion object {
        private const val LOCATION_INTERVAL_MS = 10_000L
    }
}
