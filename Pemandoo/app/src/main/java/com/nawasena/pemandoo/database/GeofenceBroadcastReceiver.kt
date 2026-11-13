package com.nawasena.pemandoo.database

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nawasena.pemandoo.R
import com.nawasena.pemandoo.data.PemandooDatabase
import com.nawasena.pemandoo.util.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = GeofencingEvent.fromIntent(intent) ?: return@launch

                if (event.hasError()) {
                    val message = GeofenceStatusCodes.getStatusCodeString(event.errorCode)
                    Log.e(TAG, "Geofence error: $message")
                    return@launch
                }

                val transition = event.geofenceTransition
                if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
                    transition != Geofence.GEOFENCE_TRANSITION_DWELL
                ) {
                    return@launch
                }

                event.triggeringGeofences.orEmpty().forEach { geofence ->
                    val landmark = PemandooDatabase.get(appContext)
                        .landmarkDao()
                        .getById(geofence.requestId)

                    if (landmark != null) {
                        AppPreferences.setLastLandmarkId(appContext, landmark.id)
                        showNotification(
                            context = appContext,
                            title = "Kamu tiba di ${landmark.name}",
                            message = "Ketuk untuk memulai tur audio tempat wisata ini.",
                            landmarkId = landmark.id
                        )
                    } else {
                        Log.w(TAG, "Landmark ${geofence.requestId} tidak ditemukan")
                    }
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Gagal memproses geofence", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        title: String,
        message: String,
        landmarkId: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MapsActivity::class.java).apply {
            putExtra(EXTRA_LANDMARK_ID, landmarkId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            landmarkId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_notifications_active_24)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (landmarkId.hashCode() and 0x7FFFFFFF).takeIf { it != 0 } ?: NOTIFICATION_ID,
                notification
            )
        } catch (exception: SecurityException) {
            Log.e(TAG, "Tidak dapat menampilkan notifikasi", exception)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tur Audio Pemandoo",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pemberitahuan saat kamu berada di dekat tempat wisata"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "GeofenceBroadcast"
        private const val CHANNEL_ID = "pemandoo_geofence_v1"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_GEOFENCE_EVENT = "com.nawasena.pemandoo.GEOFENCE_EVENT"
        const val EXTRA_LANDMARK_ID = "landmark_id"
    }
}
