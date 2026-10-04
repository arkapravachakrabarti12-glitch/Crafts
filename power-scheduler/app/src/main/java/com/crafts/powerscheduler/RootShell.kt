package com.crafts.powerscheduler

import java.util.concurrent.TimeUnit

/** Runs shell commands through `su`. All calls block, so keep them off the main thread. */
object RootShell {
    private const val RTC_WAKEALARM = "/sys/class/rtc/rtc0/wakealarm"

    data class Result(val exitCode: Int, val output: String)

    fun run(vararg commands: String, timeoutSeconds: Long = 15): Result {
        return try {
            val process = ProcessBuilder("su").redirectErrorStream(true).start()
            process.outputStream.bufferedWriter().use { w ->
                commands.forEach { w.write(it); w.write("\n") }
                w.write("exit\n")
            }
            val output = process.inputStream.bufferedReader().readText()
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroy()
                Result(-1, output)
            } else {
                Result(process.exitValue(), output)
            }
        } catch (e: Exception) {
            Result(-1, e.message ?: e.toString())
        }
    }

    fun hasRoot(): Boolean = run("id").output.contains("uid=0")

    /**
     * Shuts the phone down. When [wakeAtMillis] is set, the hardware RTC alarm is
     * programmed first so phones that honour it boot themselves at that time.
     * Returns only if the shutdown failed.
     */
    fun shutdown(wakeAtMillis: Long?): Result {
        val commands = mutableListOf<String>()
        if (wakeAtMillis != null) {
            commands += "echo 0 > $RTC_WAKEALARM"
            commands += "echo ${wakeAtMillis / 1000} > $RTC_WAKEALARM"
        }
        // Try a graceful shutdown first, then progressively blunter fallbacks.
        commands += "svc power shutdown || reboot -p || setprop sys.powerctl shutdown"
        return run(*commands.toTypedArray(), timeoutSeconds = 30)
    }
}
