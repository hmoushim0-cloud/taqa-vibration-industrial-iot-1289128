package sa.factory.taqavibrationindus.data.repository

import sa.factory.taqavibrationindus.domain.repository.TaqaVibrationRepository
import sa.factory.taqavibrationindus.hardware.HardwareSensorDriver
import sa.factory.taqavibrationindus.model.TaqaVibrationItem
import sa.factory.taqavibrationindus.model.TaqaVibrationOrder
import sa.factory.taqavibrationindus.model.TaqaVibrationTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaqaVibrationRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : TaqaVibrationRepository {

    private val _catalog = MutableStateFlow<List<TaqaVibrationItem>>(listOf(
        TaqaVibrationItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع 3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe", 180.0, "معتمد", "الخدمات الرئيسية"),
        TaqaVibrationItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        TaqaVibrationItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<TaqaVibrationItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<TaqaVibrationItem>>(emptyList())
    override val activeCart: StateFlow<List<TaqaVibrationItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<TaqaVibrationTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            TaqaVibrationTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: TaqaVibrationItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: TaqaVibrationItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): TaqaVibrationOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = TaqaVibrationOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
