package sa.factory.taqavibrationindus.data.remote

import sa.factory.taqavibrationindus.model.TaqaVibrationItem
import sa.factory.taqavibrationindus.model.TaqaVibrationOrder

interface TaqaVibrationApiService {
    suspend fun fetchCatalog(): List<TaqaVibrationItem>
    suspend fun submitOrder(order: TaqaVibrationOrder): Boolean
}
