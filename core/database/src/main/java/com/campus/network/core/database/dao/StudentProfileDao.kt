package com.campus.network.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.campus.network.core.database.entity.StudentProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentProfileDao {
    @Query("SELECT * FROM campusprofile LIMIT 1")
    fun getProfile(): Flow<StudentProfileEntity?>

    @Query("SELECT * FROM campusprofile LIMIT 1")
    suspend fun getProfileSync(): StudentProfileEntity?

    @Query("SELECT * FROM campusprofile WHERE LOWER(universityEmail) = LOWER(:email) LIMIT 1")
    suspend fun getProfileByEmail(email: String): StudentProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: StudentProfileEntity)

    @Query("UPDATE campusprofile SET syncStatus = :status, syncId = :syncId WHERE id = :id")
    suspend fun updateProfileSyncStatus(id: String, status: String, syncId: String?)

    @Query("SELECT * FROM campusprofile WHERE syncStatus = 'FAILED' LIMIT 1")
    suspend fun getFailedProfile(): StudentProfileEntity?

    @Query("SELECT * FROM campusprofile WHERE syncStatus = 'PENDING' LIMIT 1")
    suspend fun getPendingProfile(): StudentProfileEntity?

    @Query("DELETE FROM campusprofile")
    suspend fun clearProfile()
}
