package sa.factory.taqavibrationindus.domain.usecase

import sa.factory.taqavibrationindus.domain.repository.TaqaVibrationRepository
import sa.factory.taqavibrationindus.model.TaqaVibrationItem
import sa.factory.taqavibrationindus.model.TaqaVibrationOrder
import sa.factory.taqavibrationindus.model.TaqaVibrationTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: TaqaVibrationRepository) {
    operator fun invoke(): Flow<List<TaqaVibrationItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: TaqaVibrationRepository) {
    val cart = repository.activeCart
    fun add(item: TaqaVibrationItem) = repository.addToCart(item)
    fun remove(item: TaqaVibrationItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: TaqaVibrationRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): TaqaVibrationOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: TaqaVibrationRepository) {
    operator fun invoke(): Flow<TaqaVibrationTelemetry> = repository.telemetryFlow
}
