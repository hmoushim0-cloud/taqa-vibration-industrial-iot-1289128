package sa.factory.taqavibrationindus

import sa.factory.taqavibrationindus.data.repository.TaqaVibrationRepositoryImpl
import sa.factory.taqavibrationindus.hardware.HardwareSensorDriver
import sa.factory.taqavibrationindus.hardware.Iso10816SeverityClass
import sa.factory.taqavibrationindus.hardware.ModbusRtuAcousticDriverEngine
import sa.factory.taqavibrationindus.hardware.VibrationFftAnomalyEngine
import sa.factory.taqavibrationindus.model.TaqaVibrationItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TaqaVibrationRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = TaqaVibrationRepositoryImpl(sensorDriver)

        val item1 = TaqaVibrationItem("t1", "خدمة تجريبية أ", "الوصف", 100.0, "معتمد", "عام")
        val item2 = TaqaVibrationItem("t2", "خدمة تجريبية ب", "الوصف", 200.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testModbusCrc16CalculationAndValidation() {
        val engine = ModbusRtuAcousticDriverEngine()

        // Standard Modbus test vector: Read Holding Registers request: 01 03 00 00 00 02
        val request = engine.buildReadHoldingRequest(slaveAddress = 1, startRegister = 0, registerCount = 2)
        assertEquals(8, request.size)
        assertEquals(1.toByte(), request[0])
        assertEquals(3.toByte(), request[1])

        // Verify CRC matches standard expectation
        assertTrue("Request frame CRC must verify", ModbusRtuAcousticDriverEngine.verifyFrameCrc(request))
    }

    @Test
    fun testModbusFrameSynthesisAndDecoding() {
        val engine = ModbusRtuAcousticDriverEngine()

        val frame = engine.synthesizeResponseFrame(
            slaveAddress = 1,
            vx = 1.45,
            vy = 1.20,
            vz = 0.95,
            peakAcc = 2.85,
            crest = 3.2,
            tempC = 48.5,
            splDb = 72.4
        )

        assertEquals(19, frame.size)
        assertTrue(ModbusRtuAcousticDriverEngine.verifyFrameCrc(frame))

        val reading = engine.decodeSensorResponse(frame)
        assertTrue(reading.isValidCrc)
        assertEquals(1, reading.slaveAddress)
        assertEquals(1.45, reading.velocityX, 0.01)
        assertEquals(1.20, reading.velocityY, 0.01)
        assertEquals(0.95, reading.velocityZ, 0.01)
        assertEquals(2.85, reading.peakAccelerationG, 0.01)
        assertEquals(3.2, reading.crestFactor, 0.01)
        assertEquals(48.5, reading.surfaceTempCelsius, 0.01)
        assertEquals(72.4, reading.acousticSplDb, 0.01)
        assertTrue(reading.resultantVelocity > 2.0)
    }

    @Test
    fun testIso10816VibrationSeverityTriage() {
        val engine = VibrationFftAnomalyEngine()

        // Zone A: Excellent (< 1.8 mm/s)
        val diagA = engine.diagnoseMachineHealth(0.8, 0.6, 0.4)
        assertEquals(Iso10816SeverityClass.ZONE_A, diagA.severityClass)
        assertFalse(diagA.isAlarmActive)
        assertEquals(98, diagA.healthScorePct)

        // Zone B: Acceptable (1.8 to 4.5 mm/s)
        val diagB = engine.diagnoseMachineHealth(2.0, 1.5, 1.2)
        assertEquals(Iso10816SeverityClass.ZONE_B, diagB.severityClass)
        assertFalse(diagB.isAlarmActive)
        assertEquals(84, diagB.healthScorePct)

        // Zone C: Unsatisfactory (4.5 to 7.1 mm/s)
        val diagC = engine.diagnoseMachineHealth(3.8, 3.2, 2.5, acousticDb = 78.0)
        assertEquals(Iso10816SeverityClass.ZONE_C, diagC.severityClass)
        assertTrue(diagC.isAlarmActive)
        assertEquals(58, diagC.healthScorePct)
        assertTrue(diagC.primaryFaultDiagnosis.contains("BPFO"))

        // Zone D: Unacceptable (> 7.1 mm/s)
        val diagD = engine.diagnoseMachineHealth(6.0, 5.5, 4.0)
        assertEquals(Iso10816SeverityClass.ZONE_D, diagD.severityClass)
        assertTrue(diagD.isAlarmActive)
        assertEquals(22, diagD.healthScorePct)
        assertTrue(diagD.recommendedAction.contains("EMERGENCY SHUTDOWN"))
    }

    @Test
    fun testFftSpectralPeakAndHarmonicAnalysis() {
        val engine = VibrationFftAnomalyEngine()

        // Synthesize 32-point time series with 30 Hz fundamental tone
        val samples = List(32) { i ->
            val t = i.toDouble() / 1000.0
            2.0 * kotlin.math.sin(2.0 * Math.PI * 30.0 * t) + 0.8 * kotlin.math.sin(2.0 * Math.PI * 60.0 * t)
        }

        val peaks = engine.computeFftMagnitudes(samples, samplingRateHz = 1000.0)
        assertTrue("Spectral peaks list must not be empty", peaks.isNotEmpty())
        assertTrue("Top peak amplitude must be positive", peaks.first().amplitudeMmPerSec > 0.0)
    }

    @Test
    fun testHardwareSensorDriverIntegration() = runBlocking {
        val driver = HardwareSensorDriver()
        assertNotNull(driver.modbusEngine)
        assertNotNull(driver.anomalyEngine)

        val firstFrame = driver.observePeripheralStream().first()
        assertEquals("CONNECTING", firstFrame.connectionState)
        assertTrue(firstFrame.peripheralName.contains("3-Axis Vibration"))
    }
}
