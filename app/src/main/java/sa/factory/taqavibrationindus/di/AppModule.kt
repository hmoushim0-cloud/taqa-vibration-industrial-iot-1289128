package sa.factory.taqavibrationindus.di

import sa.factory.taqavibrationindus.data.repository.TaqaVibrationRepositoryImpl
import sa.factory.taqavibrationindus.domain.repository.TaqaVibrationRepository
import sa.factory.taqavibrationindus.domain.usecase.*
import sa.factory.taqavibrationindus.hardware.HardwareSensorDriver
import sa.factory.taqavibrationindus.ui.viewmodel.TaqaVibrationViewModel

class DiContainer {
    val sensorDriver = HardwareSensorDriver()
    val repository: TaqaVibrationRepository = TaqaVibrationRepositoryImpl(sensorDriver)

    val getCatalogUseCase = GetCatalogUseCase(repository)
    val manageCartUseCase = ManageCartUseCase(repository)
    val processCheckoutUseCase = ProcessCheckoutUseCase(repository)
    val observeHardwareTelemetryUseCase = ObserveHardwareTelemetryUseCase(repository)

    fun provideViewModel(): TaqaVibrationViewModel = TaqaVibrationViewModel(
        getCatalogUseCase,
        manageCartUseCase,
        processCheckoutUseCase,
        observeHardwareTelemetryUseCase
    )
}
