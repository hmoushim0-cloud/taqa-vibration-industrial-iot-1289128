package sa.factory.taqavibrationindus.hardware

import kotlin.math.*

/**
 * Modbus RTU RS-485 Frame Protocol & Acoustic Sensor Driver Engine
 * Implements standard industrial Modbus RTU master framing with CRC-16 (0xA001 polynomial),
 * register mapping for 3-axis vibration velocities, acoustic probe dB SPL, and temperature telemetry.
 */

data class ModbusRegisterReading(
    val slaveAddress: Int,
    val functionCode: Int,
    val velocityX: Double,       // mm/s RMS
    val velocityY: Double,       // mm/s RMS
    val velocityZ: Double,       // mm/s RMS
    val resultantVelocity: Double,// mm/s RMS
    val peakAccelerationG: Double, // g
    val crestFactor: Double,
    val surfaceTempCelsius: Double,
    val acousticSplDb: Double,
    val rawFrameHex: String,
    val isValidCrc: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

class ModbusRtuAcousticDriverEngine {

    companion object {
        const val FUNCTION_READ_HOLDING = 0x03
        const val FUNCTION_READ_INPUT = 0x04
        const val FUNCTION_WRITE_MULTIPLE = 0x10
        const val CRC_POLYNOMIAL = 0xA001

        /**
         * Calculates standard Modbus RTU CRC-16 (0xA001 polynomial, initial 0xFFFF).
         */
        fun calculateCrc16(data: ByteArray, length: Int = data.size): Int {
            var crc = 0xFFFF
            for (i in 0 until length) {
                crc = crc xor (data[i].toInt() and 0xFF)
                for (j in 0 until 8) {
                    crc = if ((crc and 0x0001) != 0) {
                        (crc ushr 1) xor CRC_POLYNOMIAL
                    } else {
                        crc ushr 1
                    }
                }
            }
            return crc and 0xFFFF
        }

        /**
         * Verifies if the frame trailing 2 bytes match the CRC-16 checksum (low-byte first).
         */
        fun verifyFrameCrc(frame: ByteArray): Boolean {
            if (frame.size < 4) return false
            val payloadLen = frame.size - 2
            val calculated = calculateCrc16(frame, payloadLen)
            val lowByte = (frame[payloadLen].toInt() and 0xFF)
            val highByte = (frame[payloadLen + 1].toInt() and 0xFF)
            val expected = lowByte or (highByte shl 8)
            return calculated == expected
        }
    }

    /**
     * Builds a Modbus RTU Read Holding Registers request packet (Function Code 0x03).
     */
    fun buildReadHoldingRequest(slaveAddress: Int, startRegister: Int, registerCount: Int): ByteArray {
        val buffer = ByteArray(8)
        buffer[0] = slaveAddress.toByte()
        buffer[1] = FUNCTION_READ_HOLDING.toByte()
        buffer[2] = ((startRegister ushr 8) and 0xFF).toByte()
        buffer[3] = (startRegister and 0xFF).toByte()
        buffer[4] = ((registerCount ushr 8) and 0xFF).toByte()
        buffer[5] = (registerCount and 0xFF).toByte()

        val crc = calculateCrc16(buffer, 6)
        buffer[6] = (crc and 0xFF).toByte()        // Low byte first
        buffer[7] = ((crc ushr 8) and 0xFF).toByte() // High byte

        return buffer
    }

    /**
     * Decodes a Modbus RTU response packet containing sensor telemetry registers.
     */
    fun decodeSensorResponse(frame: ByteArray): ModbusRegisterReading {
        val isValid = verifyFrameCrc(frame)
        val hex = frame.joinToString("") { String.format("%02X", it) }

        if (!isValid || frame.size < 19) {
            return ModbusRegisterReading(
                slaveAddress = if (frame.isNotEmpty()) frame[0].toInt() and 0xFF else 0,
                functionCode = if (frame.size > 1) frame[1].toInt() and 0xFF else 0,
                velocityX = 0.0,
                velocityY = 0.0,
                velocityZ = 0.0,
                resultantVelocity = 0.0,
                peakAccelerationG = 0.0,
                crestFactor = 0.0,
                surfaceTempCelsius = 0.0,
                acousticSplDb = 0.0,
                rawFrameHex = hex,
                isValidCrc = isValid
            )
        }

        val slave = frame[0].toInt() and 0xFF
        val func = frame[1].toInt() and 0xFF
        val byteCount = frame[2].toInt() and 0xFF

        // Helper to extract 16-bit register (big-endian)
        fun readReg(offset: Int): Int {
            val h = frame[offset].toInt() and 0xFF
            val l = frame[offset + 1].toInt() and 0xFF
            return (h shl 8) or l
        }

        val vx = readReg(3) / 100.0
        val vy = readReg(5) / 100.0
        val vz = readReg(7) / 100.0
        val vRes = sqrt(vx * vx + vy * vy + vz * vz)
        val peakAcc = readReg(9) / 100.0
        val crest = readReg(11) / 10.0
        val temp = readReg(13) / 10.0
        val spl = readReg(15) / 10.0

        return ModbusRegisterReading(
            slaveAddress = slave,
            functionCode = func,
            velocityX = vx,
            velocityY = vy,
            velocityZ = vz,
            resultantVelocity = (vRes * 100.0).roundToInt() / 100.0,
            peakAccelerationG = peakAcc,
            crestFactor = crest,
            surfaceTempCelsius = temp,
            acousticSplDb = spl,
            rawFrameHex = hex,
            isValidCrc = true
        )
    }

    /**
     * Synthesizes an authentic Modbus RTU telemetry response frame with calculated CRC-16.
     */
    fun synthesizeResponseFrame(
        slaveAddress: Int = 1,
        vx: Double = 1.45,
        vy: Double = 1.20,
        vz: Double = 0.95,
        peakAcc: Double = 2.85,
        crest: Double = 3.2,
        tempC: Double = 48.5,
        splDb: Double = 72.4
    ): ByteArray {
        val byteCount = 14
        val frame = ByteArray(3 + byteCount + 2) // Header(3) + Data(14) + CRC(2) = 19 bytes
        frame[0] = slaveAddress.toByte()
        frame[1] = FUNCTION_READ_HOLDING.toByte()
        frame[2] = byteCount.toByte()

        fun putReg(offset: Int, value: Int) {
            frame[offset] = ((value ushr 8) and 0xFF).toByte()
            frame[offset + 1] = (value and 0xFF).toByte()
        }

        putReg(3, (vx * 100).roundToInt())
        putReg(5, (vy * 100).roundToInt())
        putReg(7, (vz * 100).roundToInt())
        putReg(9, (peakAcc * 100).roundToInt())
        putReg(11, (crest * 10).roundToInt())
        putReg(13, (tempC * 10).roundToInt())
        putReg(15, (splDb * 10).roundToInt())

        val crc = calculateCrc16(frame, 3 + byteCount)
        frame[17] = (crc and 0xFF).toByte()
        frame[18] = ((crc ushr 8) and 0xFF).toByte()

        return frame
    }
}
