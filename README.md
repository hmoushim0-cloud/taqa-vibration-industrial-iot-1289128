# تطبيق طاقة التنبؤية — منظومة إنترنت الأشياء الصناعية والصيانة التنبؤية (`taqa-vibration-industrial-iot-1289128`)
> منظومة industrial_iot المتكاملة مع ربط 3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe

- **Project ID**: `#1289128`
- **Commercial Archetype**: `industrial_iot`
- **Bound Hardware Peripheral**: `3-Axis Vibration Accelerometer + Modbus RS485 Acoustic Probe`
- **Package Name**: `sa.factory.taqavibrationindus`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update - Iteration 38]

### Architectural Innovations & Hardware Drivers

1. **`ModbusRtuAcousticDriverEngine.kt`**:
   - Conforms to industrial serial Modbus RTU RS-485 master/slave framing standards.
   - Standard 16-bit CRC generator and validator (0xA001 polynomial, initial 0xFFFF, low-byte first).
   - High-precision 16-bit register telemetry map:
     - 3-axis RMS velocity components: X, Y, Z (mm/s * 100).
     - Peak acceleration (g * 100) and crest factor (* 10).
     - Surface RTD thermocouple temperature (Celsius * 10).
     - Acoustic emission sound pressure level (dB SPL * 10).

2. **`VibrationFftAnomalyEngine.kt`**:
   - Implements ISO 10816-3 machinery vibration evaluation criteria for rotating machinery:
     - `ZONE_A`: Excellent / newly commissioned machinery (< 1.8 mm/s RMS).
     - `ZONE_B`: Unrestricted long-term continuous operation (1.8 - 4.5 mm/s RMS).
     - `ZONE_C`: Unsatisfactory operation / scheduled overhaul required (4.5 - 7.1 mm/s RMS).
     - `ZONE_D`: Critical vibration / emergency automated trip mandated (> 7.1 mm/s RMS).
   - Discrete Fourier Transform (DFT) spectral peak extraction for fundamental rotational speed (1X RPM), misalignment harmonics (2X RPM), and bearing outer race defect frequencies (BPFO).
   - Machine health index scoring (0 to 100%) and automated alarm triage.

3. **`HardwareSensorDriver.kt`**:
   - Asynchronous Kotlin Flow pipeline continuously synthesizing and decoding validated Modbus RTU telemetry frames with real-time ISO 10816-3 diagnostics.

4. **Automated Verification Suite (`TaqaVibrationRegressionTest.kt`)**:
   - `testCheckoutCalculationAndTax`: Commercial transaction and VAT (15%) ledger verification.
   - `testModbusCrc16CalculationAndValidation`: Modbus RTU CRC-16 math and frame validation.
   - `testModbusFrameSynthesisAndDecoding`: 19-byte telemetry register packet parsing.
   - `testIso10816VibrationSeverityTriage`: Diagnostic zone transitions and trip recommendations.
   - `testFftSpectralPeakAndHarmonicAnalysis`: Spectral peak magnitude and harmonic label detection.
   - `testHardwareSensorDriverIntegration`: Real-time telemetry flow emission.

---

### Build & Verification Commands

```bash
# Run unit regression test suite
./gradlew testDebugUnitTest

# Assemble production APK
./gradlew assembleDebug
```
