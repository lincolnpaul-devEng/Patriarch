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

    @Query("DELETE FROM campusprofile")
    suspend fun clearProfile()
}
