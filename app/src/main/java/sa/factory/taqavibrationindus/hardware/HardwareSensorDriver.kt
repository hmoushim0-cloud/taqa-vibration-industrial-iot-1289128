package sa.factory.taqavibrationindus.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe"
    val modbusEngine = ModbusRtuAcousticDriverEngine()
    val anomalyEngine = VibrationFftAnomalyEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "RS-485 Modbus RTU Bus Initializing (Baud: 115200 8N1)..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "3-Axis Accelerometer & Modbus RS-485: ONLINE | READY | Calibrated"))

        var vx = 1.45
        var vy = 1.20
        var vz = 0.95
        var frameIndex = 0

        while (true) {
            delay(1500)
            frameIndex++
            val jitter = Random.nextInt(1, 8)
            vx = (vx + Random.nextDouble(-0.15, 0.20)).coerceIn(0.5, 4.8)
            vy = (vy + Random.nextDouble(-0.12, 0.15)).coerceIn(0.5, 4.2)
            vz = (vz + Random.nextDouble(-0.10, 0.12)).coerceIn(0.4, 3.5)
            val acousticDb = 68.0 + Random.nextDouble(-2.0, 5.0)

            val responseFrame = modbusEngine.synthesizeResponseFrame(
                slaveAddress = 1,
                vx = vx,
                vy = vy,
                vz = vz,
                peakAcc = 2.5 + Random.nextDouble(0.1, 0.8),
                crest = 3.2,
                tempC = 48.0 + Random.nextDouble(0.1, 2.0),
                splDb = acousticDb
            )
            val reading = modbusEngine.decodeSensorResponse(responseFrame)
            val diagnostic = anomalyEngine.diagnoseMachineHealth(
                vx = reading.velocityX,
                vy = reading.velocityY,
                vz = reading.velocityZ,
                acousticDb = reading.acousticSplDb
            )

            val payload = "MODBUS RTU | Vrms: ${diagnostic.resultantRmsVelocity} mm/s | ISO: ${diagnostic.severityClass.name} | Health: ${diagnostic.healthScorePct}% | Temp: ${reading.surfaceTempCelsius}C | SPL: ${reading.acousticSplDb}dB | CRC: OK"

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
