package com.seldon.fleet

import com.seldon.fleet.data.EnergyNormalizer
import com.seldon.fleet.domain.ClassifyDevice
import com.seldon.fleet.domain.Device
import com.seldon.fleet.domain.DeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifierTest {
    private val classify = ClassifyDevice()

    @Test
    fun normalizesEnergyBeforeDomain() {
        assertEquals(700.0, EnergyNormalizer.toMah(700.0, "mAh", null))
        assertEquals(700.0, EnergyNormalizer.toMah(2450.0, "mWh", 3.5))
    }

    @Test
    fun invalidDivisorsBecomeIndeterminate() {
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(capacity = 0)).status)
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(screen = 0)).status)
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(screen = null)).status)
        assertEquals(DeviceStatus.INDETERMINADO, classify(device(energyMah = null)).status)
    }

    @Test
    fun attentionBoundaryUsesGreaterOrEqual() {
        assertEquals(DeviceStatus.OK, classify(device(energyMah = 209.99)).status)
        assertEquals(DeviceStatus.ATENCAO, classify(device(energyMah = 210.01)).status)
    }

    @Test
    fun criticalBoundaryUsesGreaterOrEqual() {
        assertEquals(DeviceStatus.ATENCAO, classify(device(energyMah = 359.99)).status)
        assertEquals(DeviceStatus.CRITICO, classify(device(energyMah = 360.01)).status)
    }

    @Test
    fun criticalSignalsHavePrecedence() {
        assertEquals(DeviceStatus.CRITICO, classify(device(crashes = 3, jank = 0.0)).status)
    }

    private fun device(
        capacity: Int? = 3000,
        screen: Int? = 60,
        energyMah: Double? = 180.0,
        crashes: Int? = 0,
        jank: Double? = 0.0
    ) = Device(
        id = "DEV-TEST",
        model = "Test",
        soc = "Test SoC",
        tier = "mid",
        ramMb = 4096,
        batteryLevelPct = 50,
        androidApi = 34,
        batteryCapacityMah = capacity,
        screenOnMinutes = screen,
        energyMah = energyMah,
        jankFramesPct = jank,
        crashCount7d = crashes,
        lastSeen = "now"
    )
}
