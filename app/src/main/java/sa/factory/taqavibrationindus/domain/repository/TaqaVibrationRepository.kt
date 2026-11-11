package sa.factory.taqavibrationindus.domain.repository

import sa.factory.taqavibrationindus.model.TaqaVibrationItem
import sa.factory.taqavibrationindus.model.TaqaVibrationOrder
import sa.factory.taqavibrationindus.model.TaqaVibrationTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TaqaVibrationRepository {
    val catalogFlow: Flow<List<TaqaVibrationItem>>
    val activeCart: StateFlow<List<TaqaVibrationItem>>
    val telemetryFlow: Flow<TaqaVibrationTelemetry>
    
    fun addToCart(item: TaqaVibrationItem)
    fun removeFromCart(item: TaqaVibrationItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): TaqaVibrationOrder
    suspend fun refreshCatalog()
}
