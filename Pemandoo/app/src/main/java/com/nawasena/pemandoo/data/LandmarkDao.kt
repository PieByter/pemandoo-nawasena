package com.nawasena.pemandoo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LandmarkDao {

    @Query("SELECT * FROM landmarks ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Landmark>>

    @Query("SELECT * FROM landmarks WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<Landmark?>

    @Query("SELECT * FROM landmarks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Landmark?

    @Query("SELECT * FROM landmarks")
    suspend fun getAll(): List<Landmark>

    @Query("SELECT COUNT(*) FROM landmarks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(landmarks: List<Landmark>)

    @Upsert
    suspend fun upsert(landmark: Landmark)

    @Query("DELETE FROM landmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}
