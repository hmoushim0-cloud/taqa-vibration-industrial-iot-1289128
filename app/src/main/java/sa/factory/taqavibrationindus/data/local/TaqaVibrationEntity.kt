package sa.factory.taqavibrationindus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.taqavibrationindus.model.TaqaVibrationItem

@Entity(tableName = "industrial_iot_items_cache")
data class TaqaVibrationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): TaqaVibrationItem = TaqaVibrationItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
