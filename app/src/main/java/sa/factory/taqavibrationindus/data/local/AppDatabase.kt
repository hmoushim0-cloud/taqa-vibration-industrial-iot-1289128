package sa.factory.taqavibrationindus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TaqaVibrationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun industrial_iotDao(): TaqaVibrationDao
}
