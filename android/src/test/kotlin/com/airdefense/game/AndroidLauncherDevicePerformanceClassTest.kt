package com.airdefense.game

import org.junit.Test
import kotlin.test.assertEquals

class AndroidLauncherDevicePerformanceClassTest {
    @Test
    fun `low ram devices always resolve to low`() {
        val resolved =
            resolveDevicePerformanceClass(
                isLowRamDevice = true,
                heapClassMb = 512,
                highHeapClassMb = 384,
                midHeapClassMb = 256,
            )

        assertEquals(DevicePerformanceClass.LOW, resolved)
    }

    @Test
    fun `heap at or above high threshold resolves to high`() {
        val resolved =
            resolveDevicePerformanceClass(
                isLowRamDevice = false,
                heapClassMb = 384,
                highHeapClassMb = 384,
                midHeapClassMb = 256,
            )

        assertEquals(DevicePerformanceClass.HIGH, resolved)
    }

    @Test
    fun `heap at or above mid threshold resolves to mid`() {
        val resolved =
            resolveDevicePerformanceClass(
                isLowRamDevice = false,
                heapClassMb = 320,
                highHeapClassMb = 384,
                midHeapClassMb = 256,
            )

        assertEquals(DevicePerformanceClass.MID, resolved)
    }

    @Test
    fun `heap below thresholds resolves to low`() {
        val resolved =
            resolveDevicePerformanceClass(
                isLowRamDevice = false,
                heapClassMb = 192,
                highHeapClassMb = 384,
                midHeapClassMb = 256,
            )

        assertEquals(DevicePerformanceClass.LOW, resolved)
    }
}
