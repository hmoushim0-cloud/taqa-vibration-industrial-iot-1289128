package sa.factory.taqavibrationindus.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaqaVibrationDao {
    @Query("SELECT * FROM industrial_iot_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<TaqaVibrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TaqaVibrationEntity>)

    @Query("DELETE FROM industrial_iot_items_cache")
    suspend fun clearAll()
}
