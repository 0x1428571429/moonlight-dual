package com.limelight

import android.net.TrafficStats
import java.util.Locale

/**
 * 本机（整台安卓设备，所有 App、所有网卡）上传/下载速率。
 * 数据来自 [TrafficStats] 的累计字节数，两次采样相减 ÷ 时间间隔；不需要任何权限。
 * 只用于双屏副屏的监控页。
 */
class DeviceNetSpeedMeter(
    private val readRxBytes: () -> Long = TrafficStats::getTotalRxBytes,
    private val readTxBytes: () -> Long = TrafficStats::getTotalTxBytes,
    private val clockMs: () -> Long = { android.os.SystemClock.elapsedRealtime() }
) {
    /** 速率，字节/秒；null 表示还没有可比较的上一次采样，或系统不支持。 */
    data class Speed(val downBytesPerSec: Long, val upBytesPerSec: Long)

    private var lastRx = -1L
    private var lastTx = -1L
    private var lastMs = 0L

    fun reset() {
        lastRx = -1L
        lastTx = -1L
    }

    fun sample(): Speed? {
        val rx = readRxBytes()
        val tx = readTxBytes()
        val now = clockMs()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            reset()
            return null
        }
        val previousRx = lastRx
        val previousTx = lastTx
        val elapsed = now - lastMs
        lastRx = rx
        lastTx = tx
        lastMs = now
        // 首次采样、时间没走、或计数器被系统重置（变小）时，本次不出数。
        if (previousRx < 0 || previousTx < 0 || elapsed <= 0 || rx < previousRx || tx < previousTx) return null
        return Speed(
            downBytesPerSec = (rx - previousRx) * 1000 / elapsed,
            upBytesPerSec = (tx - previousTx) * 1000 / elapsed
        )
    }

    companion object {
        /** 与监控页"码率"一致的写法：B/s、KB/s、MB/s（1024 进制）。 */
        fun format(bytesPerSec: Long): String {
            val b = bytesPerSec.coerceAtLeast(0)
            return when {
                b < 1024 -> "$b B/s"
                b < 1024 * 1024 -> String.format(Locale.US, if (b < 10 * 1024) "%.1f KB/s" else "%.0f KB/s", b / 1024.0)
                else -> String.format(Locale.US, "%.2f MB/s", b / (1024.0 * 1024.0))
            }
        }
    }
}
