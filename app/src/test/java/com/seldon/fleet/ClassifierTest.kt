package com.seldon.fleet

import com.seldon.fleet.domain.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifierTest {
    private fun device(energy: Double = 700.0, unit: String = "mAh", capacity: Int? = 3000, screen: Int = 60, crash: Int = 0, jank: Double = 0.0, voltage: Double? = 3.5) = Device("x", "model", "soc", "mid", 4096, 34, capacity, 50, screen, energy, unit, voltage, jank, crash, "now")
    private val classify = ClassifyDevice()
    @Test fun `mWh and mAh equivalent`() { assertEquals(classify.invoke(device(700.0)).status, classify.invoke(device(2450.0, "mWh")).status) }
    @Test fun `zero capacity is indeterminate`() { assertEquals(DeviceStatus.INDETERMINADO, classify.invoke(device(capacity = 0)).status) }
    @Test fun `zero screen time is indeterminate`() { assertEquals(DeviceStatus.INDETERMINADO, classify.invoke(device(screen = 0)).status) }
    @Test fun `just below attention`() { assertEquals(DeviceStatus.OK, classify.invoke(device(209.99)).status) }
    @Test fun `just above attention`() { assertEquals(DeviceStatus.ATENCAO, classify.invoke(device(210.01)).status) }
    @Test fun `just below critical`() { assertEquals(DeviceStatus.ATENCAO, classify.invoke(device(359.99)).status) }
    @Test fun `just above critical`() { assertEquals(DeviceStatus.CRITICO, classify.invoke(device(360.01)).status) }
    @Test fun `critical signal wins`() { assertEquals(DeviceStatus.CRITICO, classify.invoke(device(crash = 3, jank = 8.0)).status) }
}
