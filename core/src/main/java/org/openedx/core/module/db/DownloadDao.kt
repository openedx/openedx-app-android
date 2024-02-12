package org.openedx.core.module.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("DELETE FROM download_model WHERE id = :id")
    suspend fun removeDownloadModel(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadModel(vararg downloadModelEntity: DownloadModelEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDownloadModel(downloadModelEntity: DownloadModelEntity)

    @Query("SELECT * FROM download_model")
    fun readAllData() : Flow<List<DownloadModelEntity>>

    @Query("DELETE FROM download_model WHERE id in (:ids)")
    suspend fun removeAllDownloadModels(ids: List<String>)
}
