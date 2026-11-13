package com.nawasena.pemandoo.database

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.data.Landmark
import com.nawasena.pemandoo.databinding.ActivityMapsBinding
import com.nawasena.pemandoo.util.AppPreferences
import com.nawasena.pemandoo.viewmodel.LandmarkViewModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var viewModel: LandmarkViewModel

    private var googleMap: GoogleMap? = null
    private var hasCenteredOnUser = false
    private var registeredGeofenceSignature: String? = null

    private val geofencingClient: GeofencingClient by lazy {
        LocationServices.getGeofencingClient(this)
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceBroadcastReceiver.ACTION_GEOFENCE_EVENT
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(this, GEOFENCE_REQUEST_CODE, intent, flags)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val locationGranted = fineGranted || hasLocationPermission()
        if (locationGranted) {
            enableMyLocation()
            viewModel.startLocationUpdates()
            if (canUseGeofences()) {
                viewModel.landmarks.value?.let(::updateGeofences)
            }
            requestNotificationPermission()
            requestBackgroundLocationIfNeeded()
        } else {
            Toast.makeText(
                this,
                "Izin lokasi diperlukan untuk menampilkan posisi dan geofence. Kamu tetap bisa melihat peta.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Log.d(TAG, "Notification permission was not granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[LandmarkViewModel::class.java]

        setupMap()
        observeViewModel()
        requestLocationPermissionIfNeeded()
        handleLandmarkIntent(intent)
        cleanupLegacyGeofences()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLandmarkIntent(intent)
    }

    /**
     * Opens the audio tour directly when the user taps a geofence notification.
     */
    private fun handleLandmarkIntent(intent: Intent?) {
        val landmarkId = intent?.getStringExtra(GeofenceBroadcastReceiver.EXTRA_LANDMARK_ID)
        if (landmarkId.isNullOrBlank()) return
        intent.removeExtra(GeofenceBroadcastReceiver.EXTRA_LANDMARK_ID)
        AppPreferences.setLastLandmarkId(this, landmarkId)

        binding.root.post {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: return@post
            if (navHost.navController.currentDestination?.id != R.id.resultFragment) {
                navHost.navController.navigate(
                    R.id.resultFragment,
                    bundleOf("landmarkId" to landmarkId)
                )
            }
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun observeViewModel() {
        viewModel.landmarks.observe(this) { landmarks ->
            renderLandmarks(landmarks)
            if (canUseGeofences()) {
                updateGeofences(landmarks)
            }
        }

        viewModel.currentLocation.observe(this) { location ->
            if (location != null && !hasCenteredOnUser) {
                hasCenteredOnUser = true
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        DEFAULT_ZOOM
                    )
                )
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = hasLocationPermission()

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        if (hasLocationPermission()) {
            enableMyLocation()
            viewModel.startLocationUpdates()
            if (canUseGeofences()) {
                viewModel.landmarks.value?.let(::updateGeofences)
            }
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(MEDAN_CENTER, DEFAULT_ZOOM))
        }

        viewModel.currentLocation.value?.let { location ->
            hasCenteredOnUser = true
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    DEFAULT_ZOOM
                )
            )
        }

        renderLandmarks(viewModel.landmarks.value.orEmpty())
    }

    private fun renderLandmarks(landmarks: List<Landmark>) {
        val map = googleMap ?: return
        map.clear()

        landmarks.filter { it.hasCoordinates }.forEach { landmark ->
            val position = LatLng(landmark.latitude, landmark.longitude)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(landmark.name)
                    .snippet(landmark.overview.take(140))
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
            )
            map.addCircle(
                CircleOptions()
                    .center(position)
                    .radius(landmark.radiusMeters.coerceIn(50f, 2000f).toDouble())
                    .strokeColor(ContextCompat.getColor(this, R.color.primary_green))
                    .fillColor(0x220A6847)
                    .strokeWidth(3f)
            )
        }
    }

    private fun updateGeofences(landmarks: List<Landmark>) {
        if (!canUseGeofences()) return
        if (landmarks.isEmpty()) return

        val validLandmarks = landmarks
            .filter { it.hasCoordinates && it.id.isNotBlank() }
            .take(MAX_GEOFENCES)
        if (validLandmarks.isEmpty()) return

        val signature = validLandmarks.joinToString(separator = "|") {
            "${it.id}:${it.latitude}:${it.longitude}:${it.radiusMeters}"
        }
        if (signature == registeredGeofenceSignature) return
        registeredGeofenceSignature = signature

        val geofences = validLandmarks.map { landmark ->
            Geofence.Builder()
                .setRequestId(landmark.id)
                .setCircularRegion(
                    landmark.latitude,
                    landmark.longitude,
                    landmark.radiusMeters.coerceIn(50f, 2000f)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        try {
            geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
                try {
                    geofencingClient.addGeofences(request, geofencePendingIntent)
                        .addOnSuccessListener {
                            Log.d(TAG, "${geofences.size} geofence aktif")
                        }
                        .addOnFailureListener { exception ->
                            registeredGeofenceSignature = null
                            Log.e(TAG, "Gagal menambahkan geofence", exception)
                        }
                } catch (exception: SecurityException) {
                    registeredGeofenceSignature = null
                    Log.e(TAG, "Izin lokasi dicabut saat menambahkan geofence", exception)
                }
            }
        } catch (exception: SecurityException) {
            Log.e(TAG, "Izin lokasi tidak cukup untuk geofence", exception)
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        if (hasLocationPermission()) {
            viewModel.startLocationUpdates()
            requestNotificationPermission()
            requestBackgroundLocationIfNeeded()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Versions before 1.1.0 registered geofences with a different PendingIntent
     * action. Remove those stale registrations once so they cannot trigger old
     * request IDs after an upgrade.
     */
    private fun cleanupLegacyGeofences() {
        try {
            val legacyIntent = Intent(this, GeofenceBroadcastReceiver::class.java).apply {
                action = "GeofenceEvent"
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val legacyPendingIntent = PendingIntent.getBroadcast(this, 0, legacyIntent, flags)
            geofencingClient.removeGeofences(legacyPendingIntent)
        } catch (exception: Exception) {
            Log.w(TAG, "Gagal membersihkan geofence versi lama", exception)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Geofence events are delivered while the app is in the background only when
     * the user grants "Allow all the time" location access. Android 11+ does not
     * show that choice in a normal permission dialog, so we send the user to the
     * app settings screen once, with an explanation.
     */
    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            viewModel.landmarks.value?.let(::updateGeofences)
            return
        }
        if (hasBackgroundLocationPermission()) {
            viewModel.landmarks.value?.let(::updateGeofences)
            return
        }
        if (AppPreferences.wasBackgroundLocationPrompted(this)) return

        AppPreferences.markBackgroundLocationPrompted(this)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.background_location_title)
            .setMessage(R.string.background_location_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openAppSettings()
            }
            .show()
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Tidak dapat membuka pengaturan aplikasi", exception)
            Toast.makeText(this, R.string.open_settings_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableMyLocation() {
        if (!hasLocationPermission()) return
        try {
            googleMap?.isMyLocationEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        } catch (exception: SecurityException) {
            Log.e(TAG, "Tidak dapat mengaktifkan layer lokasi", exception)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun canUseGeofences(): Boolean {
        return hasFineLocationPermission() && hasBackgroundLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        if (canUseGeofences()) {
            viewModel.landmarks.value?.let(::updateGeofences)
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasLocationPermission()) {
            viewModel.startLocationUpdates()
        }
    }

    override fun onStop() {
        super.onStop()
        // Geofence tetap aktif; update lokasi periodik dihentikan agar hemat baterai.
        viewModel.stopLocationUpdates()
    }

    companion object {
        private const val TAG = "MapsActivity"
        private const val GEOFENCE_REQUEST_CODE = 1001
        private const val MAX_GEOFENCES = 90
        private const val DEFAULT_ZOOM = 14f
        private val MEDAN_CENTER = LatLng(3.5852, 98.6815)
    }
}
