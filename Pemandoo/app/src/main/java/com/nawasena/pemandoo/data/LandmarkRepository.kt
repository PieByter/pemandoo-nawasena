package com.nawasena.pemandoo.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class LandmarkRepository(
    private val dao: LandmarkDao,
    private val context: Context
) {

    val landmarks: Flow<List<Landmark>> = dao.observeAll()

    fun observeLandmark(id: String): Flow<Landmark?> = dao.observeById(id)

    suspend fun getLandmark(id: String): Landmark? = dao.getById(id)

    suspend fun getAllLandmarks(): List<Landmark> = dao.getAll()

    suspend fun save(landmark: Landmark) = dao.upsert(landmark)

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun nearestLandmark(latitude: Double, longitude: Double): Landmark? {
        return dao.getAll().minByOrNull { it.distanceTo(latitude, longitude) }
    }

    /**
     * Inserts the bundled landmarks only once. User-created places and places
     * deleted by the user are therefore never re-created on later launches.
     */
    suspend fun seedIfNeeded() {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (preferences.getBoolean(KEY_SEEDED, false)) return

        if (dao.count() == 0) {
            dao.insertAll(SeedData.landmarks)
        }
        preferences.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "pemandoo_data"
        private const val KEY_SEEDED = "seed_version_1"
    }
}
