package com.limelight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceNetSpeedMeterTest {
    private var rx = 0L
    private var tx = 0L
    private var now = 0L
    private val meter = DeviceNetSpeedMeter({ rx }, { tx }, { now })

    @Test fun firstSampleHasNoRate() {
        rx = 1000; tx = 500; now = 10_000
        assertNull(meter.sample())
    }

    @Test fun rateIsDeltaOverElapsedTime() {
        rx = 1_000; tx = 1_000; now = 10_000
        meter.sample()
        rx += 2_048_000; tx += 51_200; now += 2_000
        val speed = meter.sample()!!
        assertEquals(1_024_000L, speed.downBytesPerSec)
        assertEquals(25_600L, speed.upBytesPerSec)
    }

    @Test fun counterResetSkipsOneSample() {
        rx = 5_000; tx = 5_000; now = 1_000
        meter.sample()
        rx = 100; tx = 100; now = 2_000
        assertNull(meter.sample())
        rx = 1_124; tx = 100; now = 3_000
        assertEquals(1_024L, meter.sample()!!.downBytesPerSec)
    }

    @Test fun unsupportedReturnsNull() {
        rx = -1; tx = -1; now = 1_000
        assertNull(meter.sample())
    }

    @Test fun formatting() {
        assertEquals("512 B/s", DeviceNetSpeedMeter.format(512))
        assertEquals("2.0 KB/s", DeviceNetSpeedMeter.format(2048))
        assertEquals("396 KB/s", DeviceNetSpeedMeter.format(396 * 1024))
        assertEquals("4.33 MB/s", DeviceNetSpeedMeter.format((4.33 * 1024 * 1024).toLong()))
    }
}
