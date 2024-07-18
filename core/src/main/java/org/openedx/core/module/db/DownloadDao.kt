package org.openedx.core.module.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.openedx.core.data.model.room.OfflineXBlockProgress

@Dao
interface DownloadDao {

    @Query("DELETE FROM download_model WHERE id = :id")
    suspend fun removeDownloadModel(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadModel(downloadModelEntities: List<DownloadModelEntity>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateDownloadModel(downloadModelEntity: DownloadModelEntity)

    @Query("SELECT * FROM download_model")
    fun getAllDataFlow(): Flow<List<DownloadModelEntity>>

    @Query("SELECT * FROM download_model")
    suspend fun readAllData(): List<DownloadModelEntity>

    @Query("SELECT * FROM download_model WHERE id in (:ids)")
    fun readAllDataByIds(ids: List<String>): Flow<List<DownloadModelEntity>>

    @Query("DELETE FROM download_model WHERE id in (:ids)")
    suspend fun removeAllDownloadModels(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineXBlockProgress(offlineXBlockProgress: OfflineXBlockProgress)

    @Query("SELECT * FROM offline_x_block_progress_table WHERE id=:id")
    suspend fun getOfflineXBlockProgress(id: String): OfflineXBlockProgress?

    @Query("SELECT * FROM offline_x_block_progress_table")
    suspend fun getAllOfflineXBlockProgress(): List<OfflineXBlockProgress>

    @Query("DELETE FROM offline_x_block_progress_table WHERE id in (:ids)")
    suspend fun removeOfflineXBlockProgress(ids: List<String>)

    @Query("DELETE FROM offline_x_block_progress_table")
    suspend fun clearOfflineProgress()
}
